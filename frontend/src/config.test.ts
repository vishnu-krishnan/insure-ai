import { describe, expect, it } from 'vitest'

import { loadConfig } from './config'

const oidcEnv = {
  VITE_OIDC_AUTHORITY: 'http://localhost:8280/realms/insure-ai',
  VITE_OIDC_CLIENT_ID: 'insure-ui',
}

describe('loadConfig', () => {
  it('names the missing variable when oidc settings are absent', () => {
    expect(() => loadConfig({})).toThrow('VITE_OIDC_AUTHORITY')
  })

  it('needs no oidc settings in mock mode', () => {
    const config = loadConfig({ VITE_AUTH_MODE: 'mock' })

    expect(config.authMode).toBe('mock')
  })

  it('rejects an unknown auth mode', () => {
    expect(() => loadConfig({ ...oidcEnv, VITE_AUTH_MODE: 'bogus' })).toThrow('VITE_AUTH_MODE')
  })

  it('builds oidc settings and default api bases', () => {
    const config = loadConfig(oidcEnv, 'http://localhost:3174')

    expect(config.oidc).toEqual({
      authority: 'http://localhost:8280/realms/insure-ai',
      clientId: 'insure-ui',
      redirectUri: 'http://localhost:3174/auth/callback',
      postLogoutRedirectUri: 'http://localhost:3174/',
    })
    expect(config.apiBase).toEqual({ customers: '', quotes: '' })
  })
})
