import { describe, expect, it } from 'vitest'

import { compoundInterest, loanSchedule, recurringDeposit, simpleInterest } from './math'

describe('simpleInterest', () => {
  it('earns principal x rate x years', () => {
    const result = simpleInterest({ principal: 100000, annualRate: 8, years: 3 })

    expect(result.interest).toBeCloseTo(24000, 2)
    expect(result.maturity).toBeCloseTo(124000, 2)
  })

  it('earns nothing at a zero rate', () => {
    expect(simpleInterest({ principal: 5000, annualRate: 0, years: 10 }).interest).toBe(0)
  })
})

describe('compoundInterest', () => {
  it('compounds yearly', () => {
    const result = compoundInterest({ principal: 100000, annualRate: 10, years: 2, timesPerYear: 1 })

    expect(result.maturity).toBeCloseTo(121000, 2)
    expect(result.interest).toBeCloseTo(21000, 2)
  })

  it('compounds monthly', () => {
    const result = compoundInterest({ principal: 100000, annualRate: 12, years: 1, timesPerYear: 12 })

    expect(result.maturity).toBeCloseTo(112682.5, 1)
  })

  it('lists the balance at the end of each year', () => {
    const result = compoundInterest({ principal: 1000, annualRate: 10, years: 3, timesPerYear: 1 })

    expect(result.yearly.map((row) => Math.round(row.balance))).toEqual([1100, 1210, 1331])
    expect(result.yearly.map((row) => row.year)).toEqual([1, 2, 3])
  })
})

describe('loanSchedule', () => {
  it('computes the monthly instalment', () => {
    const result = loanSchedule({ principal: 1000000, annualRate: 9, months: 240 })

    expect(result.emi).toBeCloseTo(8997.26, 1)
    expect(result.totalPayable).toBeCloseTo(result.emi * 240, 1)
    expect(result.totalInterest).toBeCloseTo(result.totalPayable - 1000000, 1)
  })

  it('divides the principal evenly at a zero rate', () => {
    const result = loanSchedule({ principal: 120000, annualRate: 0, months: 12 })

    expect(result.emi).toBe(10000)
    expect(result.totalInterest).toBe(0)
  })

  it('pays the loan off by the end of the term', () => {
    const result = loanSchedule({ principal: 500000, annualRate: 7.5, months: 60 })

    expect(result.yearly.at(-1)?.closingBalance).toBeCloseTo(0, 2)
    expect(result.yearly).toHaveLength(5)
  })

  it('handles a term that is not a whole number of years', () => {
    const result = loanSchedule({ principal: 100000, annualRate: 10, months: 18 })

    expect(result.yearly).toHaveLength(2)
    expect(result.yearly.at(-1)?.closingBalance).toBeCloseTo(0, 2)
  })
})

describe('recurringDeposit', () => {
  it('adds up the deposits when there is no interest', () => {
    const result = recurringDeposit({ monthlyDeposit: 5000, annualRate: 0, months: 24 })

    expect(result.maturity).toBe(120000)
    expect(result.interest).toBe(0)
  })

  it('grows each deposit at the monthly rate', () => {
    const result = recurringDeposit({ monthlyDeposit: 1000, annualRate: 12, months: 12 })

    expect(result.deposited).toBe(12000)
    expect(result.maturity).toBeCloseTo(12809.33, 1)
  })
})
