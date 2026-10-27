import { describe, expect, it } from 'vitest'

import { estimatePremium } from './estimate'

describe('estimatePremium', () => {
  it('multiplies the cover by the rate for the cover type', () => {
    expect(estimatePremium('HOME', 250000, 36)).toEqual({ annual: 12500, monthly: 1041.67 })
    expect(estimatePremium('AUTO', 100000, 30)).toEqual({ annual: 7000, monthly: 583.33 })
    expect(estimatePremium('LIFE', 100000, 30)).toEqual({ annual: 9000, monthly: 750 })
    expect(estimatePremium('HEALTH', 100000, 30)).toEqual({ annual: 8000, monthly: 666.67 })
  })

  it('adds 15% for customers older than 60 but not for exactly 60', () => {
    expect(estimatePremium('HOME', 250000, 60)?.annual).toBe(12500)
    expect(estimatePremium('HOME', 250000, 61)?.annual).toBe(14375)
  })

  it('rounds half up to two decimals', () => {
    expect(estimatePremium('AUTO', 100.5, 30)).toEqual({ annual: 7.04, monthly: 0.59 })
  })

  it('returns nothing for an unknown type, a non-positive cover or an age outside 18 to 75', () => {
    expect(estimatePremium('PET', 1000, 30)).toBeNull()
    expect(estimatePremium('HOME', 0, 30)).toBeNull()
    expect(estimatePremium('HOME', 1000, 17)).toBeNull()
    expect(estimatePremium('HOME', 1000, 76)).toBeNull()
  })
})
