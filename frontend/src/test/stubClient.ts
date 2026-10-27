import { vi } from 'vitest'

import type { AuthClient, AuthUser } from '../auth/types'

export function stubClient(user: AuthUser | null) {
  return {
    restore: async () => user,
    completeSignIn: async () => ({ user: user!, returnTo: '/' }),
    signIn: vi.fn(async () => {}),
    signOut: vi.fn(async () => {}),
    getAccessToken: async () => null,
  } satisfies AuthClient
}
