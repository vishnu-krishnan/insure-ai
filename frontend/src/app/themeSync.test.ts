import { describe, expect, it } from 'vitest'

import { publishColorScheme } from './themeSync'

describe('publishColorScheme', () => {
  it('stores the chosen scheme in a cookie the sign-in page can read', () => {
    publishColorScheme('dark')
    expect(document.cookie).toContain('insure_theme=dark')

    publishColorScheme('light')
    expect(document.cookie).toContain('insure_theme=light')
    expect(document.cookie).not.toContain('insure_theme=dark')
  })
})
