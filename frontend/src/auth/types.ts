export interface AuthUser {
  name: string
  roles: string[]
}

export interface AuthClient {
  /** The signed-in user for this page load, or null when nobody is signed in. */
  restore(): Promise<AuthUser | null>
  /** Finishes the redirect back from the identity provider. */
  completeSignIn(): Promise<{ user: AuthUser; returnTo: string }>
  signIn(returnTo: string): Promise<void>
  signOut(): Promise<void>
  getAccessToken(): Promise<string | null>
}
