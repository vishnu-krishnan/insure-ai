import { createContext, useContext } from 'react'

import type { AuthUser } from './types'

export interface AuthState {
  status: 'loading' | 'authenticated' | 'anonymous'
  user?: AuthUser
  signIn(returnTo?: string): Promise<void>
  signOut(): Promise<void>
  getAccessToken(): Promise<string | null>
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return value
}
