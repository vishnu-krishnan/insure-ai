// The sign-in page is served by Keycloak on another origin that shares this host, so a cookie is the
// simplest way to tell it which colour scheme the agent chose here.
const COOKIE = 'insure_theme'

export function publishColorScheme(scheme: 'light' | 'dark'): void {
  try {
    document.cookie = `${COOKIE}=${scheme}; path=/; max-age=31536000; SameSite=Lax`
  } catch {
    // Cookies can be blocked; the sign-in page then follows the system setting.
  }
}
