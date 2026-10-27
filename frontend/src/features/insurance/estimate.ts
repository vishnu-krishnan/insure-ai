// Indicative premium for the public plan pages. It mirrors the rule the quote service applies
// (cover amount x base rate for the type, +15% above age 60); the saved quote is always the source of truth.
const RATE_BASIS_POINTS: Record<string, number> = { AUTO: 700, HOME: 500, LIFE: 900, HEALTH: 800 }

export interface Estimate {
  annual: number
  monthly: number
}

export function estimatePremium(type: string, cover: number, age: number): Estimate | null {
  const rate = RATE_BASIS_POINTS[type]
  if (rate === undefined || !Number.isFinite(cover) || cover <= 0 || !Number.isInteger(age) || age < 18 || age > 75) {
    return null
  }
  const loading = age > 60 ? 11500 : 10000
  const annualCents = Math.round((Math.round(cover * 100) * rate * loading) / 1e8)
  return { annual: annualCents / 100, monthly: Math.round(annualCents / 12) / 100 }
}
