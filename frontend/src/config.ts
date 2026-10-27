export type AuthMode = 'oidc' | 'mock'

export interface Config {
  authMode: AuthMode
  oidc: {
    authority: string
    clientId: string
    redirectUri: string
    postLogoutRedirectUri: string
  }
  apiBase: {
    customers: string
    quotes: string
  }
}

type Env = Record<string, string | undefined>

function required(env: Env, name: string): string {
  const value = env[name]?.trim()
  if (!value) {
    throw new Error(`Missing required setting ${name}`)
  }
  return value
}

export function loadConfig(env: Env, origin: string = window.location.origin): Config {
  const mode = env.VITE_AUTH_MODE?.trim() || 'oidc'
  if (mode !== 'oidc' && mode !== 'mock') {
    throw new Error(`VITE_AUTH_MODE must be "oidc" or "mock", got "${mode}"`)
  }

  const apiBase = {
    customers: env.VITE_CUSTOMER_API?.trim() ?? '',
    quotes: env.VITE_QUOTE_API?.trim() ?? '',
  }

  if (mode === 'mock') {
    return {
      authMode: mode,
      oidc: { authority: '', clientId: '', redirectUri: '', postLogoutRedirectUri: '' },
      apiBase,
    }
  }

  return {
    authMode: mode,
    oidc: {
      authority: required(env, 'VITE_OIDC_AUTHORITY'),
      clientId: required(env, 'VITE_OIDC_CLIENT_ID'),
      redirectUri: `${origin}/auth/callback`,
      postLogoutRedirectUri: `${origin}/`,
    },
    apiBase,
  }
}

let cached: Config | undefined

export function getConfig(): Config {
  cached ??= loadConfig(import.meta.env)
  return cached
}
