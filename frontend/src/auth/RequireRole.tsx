import { Center, Loader } from '@mantine/core'
import { useEffect, type ReactNode } from 'react'

import { AccessDenied } from './AccessDenied'
import { useAuth } from './useAuth'

export function RequireRole({ role, children }: { role?: string; children: ReactNode }) {
  const { status, user, signIn } = useAuth()

  useEffect(() => {
    if (status === 'anonymous') {
      void signIn()
    }
  }, [status, signIn])

  if (status !== 'authenticated') {
    return (
      <Center h="50vh">
        <Loader aria-label="Signing in" />
      </Center>
    )
  }
  if (role && !user?.roles.includes(role)) {
    return <AccessDenied />
  }
  return <>{children}</>
}
