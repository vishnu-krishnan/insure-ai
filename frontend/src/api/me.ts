import { getConfig } from '../config'
import { request } from './http'
import { ApiError } from './problem'
import {
  customerResponseSchema,
  quotePageSchema,
  quoteSummarySchema,
  type Customer,
  type CustomerPayload,
  type CustomerQuoteRequest,
  type QuotePage,
  type QuoteSummary,
} from './schemas'
import { toDetailsPayload } from '../features/portal/typeFields'

const ME = '/api/v1/me'

/** Satisfies the shared form's email rule before a profile exists; it is never sent (the server uses the account email). */
export const ACCOUNT_EMAIL_PLACEHOLDER = 'account@example.com'

/** The signed-in customer's profile, or null until they have completed it. */
export async function getMyProfile(): Promise<Customer | null> {
  try {
    return await request(getConfig().apiBase.customers, ME, customerResponseSchema)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null
    }
    throw error
  }
}

// The email belongs to the sign-in account, so it is never sent: the server takes it from the token.
export function saveMyProfile(input: CustomerPayload, exists: boolean): Promise<Customer> {
  const { email: _email, ...body } = input
  return request(getConfig().apiBase.customers, ME, customerResponseSchema, {
    method: exists ? 'PUT' : 'POST',
    body: JSON.stringify(body),
  })
}

export function listMyQuotes(page: number, size: number): Promise<QuotePage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  return request(getConfig().apiBase.quotes, `${ME}/quotes?${params}`, quotePageSchema)
}

export function getMyQuote(quoteId: string): Promise<QuoteSummary> {
  return request(getConfig().apiBase.quotes, `${ME}/quotes/${encodeURIComponent(quoteId)}`, quoteSummarySchema)
}

// Same amount handling as the agent quote form: the amount is written into the JSON exactly as typed.
export function createMyQuote(input: CustomerQuoteRequest): Promise<QuoteSummary> {
  const {
    customerId: _customerId,
    policyTermYears,
    paymentFrequency,
    deductible,
    startDate,
    details,
    ...required
  } = input
  const json = JSON.stringify({
    ...required,
    coverageAmount: '__amount__',
    ...(policyTermYears ? { policyTermYears: Number(policyTermYears) } : {}),
    ...(paymentFrequency ? { paymentFrequency } : {}),
    ...(deductible ? { deductible: Number(deductible) } : {}),
    ...(startDate ? { startDate } : {}),
    ...(Object.keys(details).length > 0 ? { details: toDetailsPayload(input.coverageType, details) } : {}),
  })
  return request(getConfig().apiBase.quotes, `${ME}/quotes`, quoteSummarySchema, {
    method: 'POST',
    body: json.replace('"__amount__"', input.coverageAmount),
  })
}
