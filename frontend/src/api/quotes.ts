import { getConfig } from '../config'
import { toDetailsPayload } from '../features/portal/typeFields'
import { request } from './http'
import {
  customerQuoteResponseSchema,
  quotePageSchema,
  quoteSummarySchema,
  type CustomerQuote,
  type CustomerQuoteRequest,
  type QuotePage,
  type QuoteSummary,
} from './schemas'

const AMOUNT_PLACEHOLDER = '"__amount__"'

// The amount is sent as a JSON number written exactly as typed, so long
// values are not rounded through a binary float. The schema has already
// limited it to digits with an optional two-decimal fraction.
function quoteBody(input: CustomerQuoteRequest): string {
  const { policyTermYears, paymentFrequency, deductible, startDate, details, ...required } = input
  const json = JSON.stringify({
    ...required,
    coverageAmount: '__amount__',
    ...(policyTermYears ? { policyTermYears: Number(policyTermYears) } : {}),
    ...(paymentFrequency ? { paymentFrequency } : {}),
    ...(deductible ? { deductible: Number(deductible) } : {}),
    ...(startDate ? { startDate } : {}),
    ...(Object.keys(details).length > 0 ? { details: toDetailsPayload(input.coverageType, details) } : {}),
  })
  return json.replace(AMOUNT_PLACEHOLDER, input.coverageAmount)
}

export function createQuote(input: CustomerQuoteRequest): Promise<CustomerQuote> {
  return request(getConfig().apiBase.quotes, '/api/v2/quotes', customerQuoteResponseSchema, {
    method: 'POST',
    body: quoteBody(input),
  })
}

export function getQuote(quoteId: string): Promise<QuoteSummary> {
  return request(getConfig().apiBase.quotes, `/api/v1/quotes/${encodeURIComponent(quoteId)}`, quoteSummarySchema)
}

export function listQuotes(page: number, size: number, text = '', coverageType = ''): Promise<QuotePage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (coverageType) {
    params.set('coverageType', coverageType)
  }
  if (text.trim()) {
    params.set('q', text.trim())
  }
  return request(getConfig().apiBase.quotes, `/api/v1/quotes?${params}`, quotePageSchema)
}
