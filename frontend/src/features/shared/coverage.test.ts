import { describe, expect, it } from 'vitest'

import { coverageStyle } from './coverage'

describe('coverage style', () => {
  it('gives each cover type its own colour', () => {
    const colours = ['AUTO', 'HOME', 'LIFE', 'HEALTH'].map((type) => coverageStyle(type).color)

    expect(new Set(colours).size).toBe(4)
  })

  it('falls back to a neutral style for an unknown type', () => {
    expect(coverageStyle('PET')).toMatchObject({ color: 'gray', label: 'PET' })
  })
})
