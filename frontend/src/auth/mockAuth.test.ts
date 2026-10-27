import { afterEach, describe, expect, it, vi } from 'vitest'

import { loadConfig } from '../config'
import { isMockAuthEnabled } from './mode'
import { createMockAuth } from './mockAuth'

afterEach(() => vi.unstubAllEnvs())

describe('mock auth', () => {
  it('signs in a fake agent', async () => {
    const user = await createMockAuth().restore()

    expect(user?.roles).toContain('agent')
  })

  it('is on only in the dev server', () => {
    const config = loadConfig({ VITE_AUTH_MODE: 'mock' })

    expect(isMockAuthEnabled(config)).toBe(true)
  })

  it('is off in a production build even when requested', () => {
    vi.stubEnv('DEV', false)
    const config = loadConfig({ VITE_AUTH_MODE: 'mock' })

    expect(isMockAuthEnabled(config)).toBe(false)
  })

  it('is off when the mode is oidc', () => {
    const config = loadConfig({ VITE_OIDC_AUTHORITY: 'http://kc/realms/r', VITE_OIDC_CLIENT_ID: 'c' })

    expect(isMockAuthEnabled(config)).toBe(false)
  })
})
