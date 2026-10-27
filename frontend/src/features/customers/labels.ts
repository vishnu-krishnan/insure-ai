export function labelFor(code: string | null | undefined): string {
  if (!code) {
    return ''
  }
  return code.charAt(0) + code.slice(1).toLowerCase()
}
