// Reads the role claim for UI routing only. The services validate the token.
export function rolesFromToken(accessToken: string): string[] {
  try {
    const payload = accessToken.split('.')[1]
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(atob(base64)) as { realm_access?: { roles?: unknown } }
    const roles = claims.realm_access?.roles
    return Array.isArray(roles) ? roles.filter((role): role is string => typeof role === 'string') : []
  } catch {
    return []
  }
}
