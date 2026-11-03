import { Alert, Center, Loader } from '@mantine/core'
import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'

import { setTokenProvider, setUnauthorizedHandler } from '../api/http'
import { getConfig } from '../config'
import { AuthContext, type AuthState } from './useAuth'
import type { AuthClient, AuthUser } from './types'

async function createClient(): Promise<AuthClient> {
  const config = getConfig()
  if (config.authMode === 'mock') {
    if (import.meta.env.DEV) {
      const { createMockAuth } = await import('./mockAuth')
      return createMockAuth()
    }
    throw new Error('Mock sign-in is only available in the development server')
  }
  const { createOidcAuth } = await import('./oidcAuth')
  return createOidcAuth(config)
}

// Tokens are kept in memory, so a page reload loses them. This marker holds no token; it only records that the
// visitor was signed in, so a reload can go back to Keycloak (which signs them straight in while its session lasts).
const SIGNED_IN_MARKER = 'insure.signedIn'

function marker(action: 'get' | 'set' | 'clear'): boolean {
  try {
    if (action === 'set') window.localStorage.setItem(SIGNED_IN_MARKER, '1')
    if (action === 'clear') window.localStorage.removeItem(SIGNED_IN_MARKER)
    return action === 'get' && window.localStorage.getItem(SIGNED_IN_MARKER) === '1'
  } catch {
    return false
  }
}

function currentPath(): string {
  return `${window.location.pathname}${window.location.search}`
}

export function AuthProvider({ children, client: provided }: { children: ReactNode; client?: AuthClient }) {
  const clientRef = useRef<AuthClient | null>(null)
  const started = useRef(false)
  const [status, setStatus] = useState<AuthState['status']>('loading')
  const [user, setUser] = useState<AuthUser | undefined>()
  const [failure, setFailure] = useState<string | null>(null)

  useEffect(() => {
    if (started.current) {
      return
    }
    started.current = true
    void (async () => {
      try {
        const client = provided ?? (await createClient())
        clientRef.current = client
        setTokenProvider(() => client.getAccessToken())
        setUnauthorizedHandler(() => void client.signIn(currentPath()))

        let signedIn: AuthUser | null
        if (window.location.pathname === '/auth/callback') {
          const result = await client.completeSignIn()
          window.history.replaceState(null, '', result.returnTo)
          signedIn = result.user
          marker('set')
        } else {
          signedIn = await client.restore()
          if (!signedIn && marker('get')) {
            marker('clear')
            await client.signIn(currentPath())
            return
          }
        }
        setUser(signedIn ?? undefined)
        setStatus(signedIn ? 'authenticated' : 'anonymous')
      } catch (error) {
        marker('clear')
        setFailure(error instanceof Error ? error.message : 'Sign-in failed')
      }
    })()
  }, [provided])

  const value = useMemo<AuthState>(
    () => ({
      status,
      user,
      signIn: async (returnTo = currentPath()) => clientRef.current?.signIn(returnTo),
      signOut: async () => {
        marker('clear')
        return clientRef.current?.signOut()
      },
      getAccessToken: async () => clientRef.current?.getAccessToken() ?? null,
    }),
    [status, user],
  )

  if (failure) {
    return (
      <Alert color="red" title="Sign-in is unavailable" m="md">
        {failure}
      </Alert>
    )
  }
  if (status === 'loading') {
    return (
      <Center h="100vh">
        <Loader aria-label="Signing in" />
      </Center>
    )
  }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
