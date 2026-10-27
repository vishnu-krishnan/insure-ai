import { rolesFromToken } from './roles'
import type { AuthClient, AuthUser } from './types'

const MOCK_TOKEN = [
  'e30',
  btoa(JSON.stringify({ realm_access: { roles: ['agent'] } }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, ''),
  'mock',
].join('.')

export function createMockAuth(): AuthClient {
  const user: AuthUser = { name: 'Mock Agent', roles: rolesFromToken(MOCK_TOKEN) }
  let signedIn = true

  return {
    restore: async () => (signedIn ? user : null),
    completeSignIn: async () => ({ user, returnTo: '/' }),
    signIn: async () => {
      signedIn = true
    },
    signOut: async () => {
      signedIn = false
    },
    getAccessToken: async () => (signedIn ? MOCK_TOKEN : null),
  }
}
