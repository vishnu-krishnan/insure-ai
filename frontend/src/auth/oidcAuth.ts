import { InMemoryWebStorage, UserManager, WebStorageStateStore, type User } from 'oidc-client-ts'

import type { Config } from '../config'
import { rolesFromToken } from './roles'
import type { AuthClient, AuthUser } from './types'

const SCOPES = ['openid', 'profile', 'quote:create', 'quote:read', 'customer:create', 'customer:read']

function toAuthUser(user: User): AuthUser {
  const profile = user.profile
  return {
    name: profile.preferred_username ?? profile.name ?? 'Signed-in user',
    roles: rolesFromToken(user.access_token),
  }
}

// Tokens live in memory only. Sign-in state (state, nonce, PKCE verifier) must
// survive the redirect to Keycloak, so it uses sessionStorage and holds no token.
export function createOidcAuth(config: Config): AuthClient {
  const manager = new UserManager({
    authority: config.oidc.authority,
    client_id: config.oidc.clientId,
    redirect_uri: config.oidc.redirectUri,
    post_logout_redirect_uri: config.oidc.postLogoutRedirectUri,
    response_type: 'code',
    scope: SCOPES.join(' '),
    automaticSilentRenew: true,
    userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
    stateStore: new WebStorageStateStore({ store: window.sessionStorage }),
  })

  return {
    restore: async () => {
      const user = await manager.getUser()
      return user && !user.expired ? toAuthUser(user) : null
    },
    completeSignIn: async () => {
      const user = await manager.signinCallback()
      if (!user) {
        throw new Error('Sign-in did not return a user')
      }
      const returnTo = typeof user.state === 'string' && user.state.startsWith('/') ? user.state : '/'
      return { user: toAuthUser(user), returnTo }
    },
    signIn: (returnTo) => manager.signinRedirect({ state: returnTo }),
    signOut: () => manager.signoutRedirect(),
    getAccessToken: async () => (await manager.getUser())?.access_token ?? null,
  }
}
