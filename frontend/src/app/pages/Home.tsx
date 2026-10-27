import { Center, Loader } from '@mantine/core'
import { Navigate } from 'react-router-dom'

import { AccessDenied } from '../../auth/AccessDenied'
import { useAuth } from '../../auth/useAuth'
import { LandingPage } from '../landing/LandingPage'

export function Home() {
  const { status, user } = useAuth()

  if (status === 'loading') {
    return (
      <Center h="50vh">
        <Loader aria-label="Loading" />
      </Center>
    )
  }
  if (status === 'anonymous') {
    return <LandingPage />
  }
  if (user?.roles.includes('agent')) {
    return <Navigate to="/agent" replace />
  }
  if (user?.roles.includes('customer')) {
    return <Navigate to="/me" replace />
  }
  return <AccessDenied />
}
