function encode(value: unknown): string {
  return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

export function fakeToken(roles: string[]): string {
  return `${encode({ alg: 'none' })}.${encode({ realm_access: { roles } })}.sig`
}
