// Illustrative finance maths for the public calculator. Plain doubles are fine here: nothing is stored or billed.

export interface InterestInput {
  principal: number
  annualRate: number
  years: number
}

export interface InterestResult {
  interest: number
  maturity: number
}

export interface YearBalance {
  year: number
  balance: number
}

export function simpleInterest({ principal, annualRate, years }: InterestInput): InterestResult {
  const interest = (principal * annualRate * years) / 100
  return { interest, maturity: principal + interest }
}

export function compoundInterest({
  principal,
  annualRate,
  years,
  timesPerYear,
}: InterestInput & { timesPerYear: number }): InterestResult & { yearly: YearBalance[] } {
  const growth = (elapsedYears: number) =>
    principal * (1 + annualRate / 100 / timesPerYear) ** (timesPerYear * elapsedYears)
  const yearly: YearBalance[] = []
  for (let year = 1; year <= Math.ceil(years); year += 1) {
    yearly.push({ year, balance: growth(Math.min(year, years)) })
  }
  const maturity = growth(years)
  return { interest: maturity - principal, maturity, yearly }
}

export interface LoanYear {
  year: number
  principalPaid: number
  interestPaid: number
  closingBalance: number
}

export function loanSchedule({
  principal,
  annualRate,
  months,
}: {
  principal: number
  annualRate: number
  months: number
}): {
  emi: number
  totalPayable: number
  totalInterest: number
  yearly: LoanYear[]
} {
  const monthlyRate = annualRate / 1200
  const emi =
    monthlyRate === 0
      ? principal / months
      : (principal * monthlyRate * (1 + monthlyRate) ** months) / ((1 + monthlyRate) ** months - 1)

  const yearly: LoanYear[] = []
  let balance = principal
  let principalPaid = 0
  let interestPaid = 0
  for (let month = 1; month <= months; month += 1) {
    const interest = balance * monthlyRate
    const repaid = emi - interest
    balance = month === months ? 0 : balance - repaid
    principalPaid += repaid
    interestPaid += interest
    if (month % 12 === 0 || month === months) {
      yearly.push({ year: Math.ceil(month / 12), principalPaid, interestPaid, closingBalance: balance })
      principalPaid = 0
      interestPaid = 0
    }
  }
  const totalPayable = emi * months
  return { emi, totalPayable, totalInterest: totalPayable - principal, yearly }
}

export function recurringDeposit({
  monthlyDeposit,
  annualRate,
  months,
}: {
  monthlyDeposit: number
  annualRate: number
  months: number
}): { deposited: number; interest: number; maturity: number } {
  const monthlyRate = annualRate / 1200
  const deposited = monthlyDeposit * months
  const maturity =
    monthlyRate === 0
      ? deposited
      : monthlyDeposit * (((1 + monthlyRate) ** months - 1) / monthlyRate) * (1 + monthlyRate)
  return { deposited, interest: maturity - deposited, maturity }
}
