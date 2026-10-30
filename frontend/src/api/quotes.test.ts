import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { server } from '../test/server'
import { createQuote, getQuote, listQuotes } from './quotes'
import { customerQuoteRequestSchema, emptyQuoteForm } from './schemas'

const customerId = '10000001'

describe('quote schema', () => {
  it('rejects a zero coverage amount', () => {
    const result = customerQuoteRequestSchema.safeParse({
      ...emptyQuoteForm,
      customerId,
      coverageType: 'AUTO',
      coverageAmount: '0',
    })

    expect(result.success).toBe(false)
  })

  it('keeps a long coverage amount as the exact string', () => {
    const result = customerQuoteRequestSchema.parse({
      ...emptyQuoteForm,
      customerId,
      coverageType: 'HOME',
      coverageAmount: '1234567890.12',
    })

    expect(result.coverageAmount).toBe('1234567890.12')
  })

  it('rejects a coverage type outside the supported list', () => {
    const result = customerQuoteRequestSchema.safeParse({
      ...emptyQuoteForm,
      customerId,
      coverageType: 'PET',
      coverageAmount: '10',
    })

    expect(result.success).toBe(false)
  })
})

describe('quote detail fields', () => {
  const base = { ...emptyQuoteForm, customerId, coverageType: 'HOME' as const, coverageAmount: '250000' }

  function problems(values: unknown): string[] {
    const result = customerQuoteRequestSchema.safeParse(values)
    return result.success ? [] : result.error.issues.map((issue) => issue.path.join('.'))
  }

  it('accepts a quote that leaves the optional details at their defaults', () => {
    expect(problems(base)).toEqual([])
  })

  it.each([
    ['policyTermYears', '0'],
    ['policyTermYears', '41'],
    ['policyTermYears', '2.5'],
    ['deductible', '-1'],
    ['deductible', '10.123'],
    ['paymentFrequency', 'WEEKLY'],
  ])('rejects %s = %s', (field, value) => {
    expect(problems({ ...base, [field]: value })).toContain(field)
  })

  it('rejects a deductible that is not below the coverage amount', () => {
    expect(problems({ ...base, deductible: '250000' })).toContain('deductible')
    expect(problems({ ...base, deductible: '249999.99' })).toEqual([])
  })

  it('rejects a start date in the past or more than a year ahead', () => {
    expect(problems({ ...base, startDate: '2000-01-01' })).toContain('startDate')
    expect(problems({ ...base, startDate: '2999-01-01' })).toContain('startDate')
  })

  it('sends the details the agent set and leaves the rest to the defaults', async () => {
    let body = ''
    server.use(
      http.post('/api/v2/quotes', async ({ request }) => {
        body = await request.text()
        return HttpResponse.json({}, { status: 500 })
      }),
    )

    await createQuote({
      ...base,
      policyTermYears: '10',
      paymentFrequency: 'MONTHLY',
      deductible: '5000',
      startDate: '',
    }).catch(() => undefined)

    expect(JSON.parse(body)).toEqual({
      customerId,
      coverageType: 'HOME',
      coverageAmount: 250000,
      policyTermYears: 10,
      paymentFrequency: 'MONTHLY',
      deductible: 5000,
    })
  })
})

describe('quote api', () => {
  it('posts the amount as the exact decimal text', async () => {
    let body = ''
    server.use(
      http.post('/api/v2/quotes', async ({ request }) => {
        body = await request.text()
        return HttpResponse.json(
          {
            quoteId: 'q-1',
            quoteNumber: 'QT-000001',
            customerNumber: 'CUS-000001',
            customerId,
            customerName: 'Asha Menon',
            coverageType: 'HOME',
            age: 36,
            coverageAmount: 1234567890.12,
            annualPremium: 61728394.51,
            monthlyPremium: 5144032.88,
          },
          { status: 201 },
        )
      }),
    )

    await createQuote({ ...emptyQuoteForm, customerId, coverageType: 'HOME', coverageAmount: '1234567890.12' })

    expect(body).toContain('"coverageAmount":1234567890.12')
  })

  it('reads a stored quote, which carries no customer ID', async () => {
    server.use(
      http.get('/api/v1/quotes/:id', () =>
        HttpResponse.json({
          quoteId: 'q-100',
          quoteNumber: 'QT-000100',
          customerNumber: null,
          customerName: 'Asha Menon',
          coverageType: 'HOME',
          age: 36,
          coverageAmount: 250000,
          annualPremium: 12500.5,
          monthlyPremium: 1041.71,
        }),
      ),
    )

    await expect(getQuote('q-100')).resolves.toMatchObject({
      quoteId: 'q-100',
      quoteNumber: 'QT-000100',
      customerName: 'Asha Menon',
    })
  })

  it('reports an unknown quote as QUOTE_NOT_FOUND', async () => {
    server.use(
      http.get('/api/v1/quotes/:id', () =>
        HttpResponse.json({ status: 404, code: 'QUOTE_NOT_FOUND' }, { status: 404 }),
      ),
    )

    await expect(getQuote('missing')).rejects.toMatchObject({ code: 'QUOTE_NOT_FOUND' })
  })

  it('requests the page and size query parameters', async () => {
    let query = ''
    server.use(
      http.get('/api/v1/quotes', ({ request }) => {
        query = new URL(request.url).search
        return HttpResponse.json({
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        })
      }),
    )

    const page = await listQuotes(0, 20)

    expect(query).toBe('?page=0&size=20')
    expect(page.totalElements).toBe(0)
  })

  it('sends the search text with the page and size', async () => {
    let query = ''
    server.use(
      http.get('/api/v1/quotes', ({ request }) => {
        query = new URL(request.url).search
        return HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
      }),
    )

    await listQuotes(1, 10, 'QT-0001 & more')

    expect(query).toBe('?page=1&size=10&q=QT-0001+%26+more')
  })
})
