import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { renderFeature } from '../../test/renderFeature'
import { server } from '../../test/server'
import { Component } from './routes'

const base = '/agent/quotes'
const customerId = '10000001'
const customer = {
  customerId,
  customerNumber: 'CUS-000001',
  fullName: 'Asha Menon',
  email: 'asha@example.com',
  phone: '+919876543210',
  dateOfBirth: '1990-04-12',
}
const quote = {
  quoteId: 'q-100',
  quoteNumber: 'QT-000100',
  customerId,
  customerNumber: 'CUS-000001',
  customerName: 'Asha Menon',
  coverageType: 'HOME',
  age: 36,
  coverageAmount: 250000,
  annualPremium: 12500.5,
  monthlyPremium: 1041.71,
}

// The stored-quote endpoint does not return the customer ID the create endpoint does.
const { customerId: _omitted, ...storedQuote } = quote

function customerFound() {
  server.use(
    http.get('/api/v1/customers/:id', () => HttpResponse.json(customer)),
    http.get('/api/v1/customers', () =>
      HttpResponse.json({ content: [customer], page: 0, size: 5, totalElements: 1, totalPages: 1 }),
    ),
  )
}

async function pickCustomer() {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Find a customer'), 'asha')
  await user.click(await screen.findByRole('button', { name: /Asha Menon/ }))
  await screen.findByText('Selected customer')
  return user
}

async function fillQuote(user: ReturnType<typeof userEvent.setup>, amount = '250000') {
  await user.selectOptions(screen.getByLabelText('Coverage type'), 'HOME')
  await user.type(screen.getByLabelText('Coverage amount'), amount)
}

describe('quote create page', () => {
  it('blocks the submit until a customer has been chosen', async () => {
    let sent = false
    server.use(
      http.post('/api/v2/quotes', () => {
        sent = true
        return HttpResponse.json(quote, { status: 201 })
      }),
    )
    customerFound()
    renderFeature(base, Component, `${base}/new`)

    const user = userEvent.setup()
    await fillQuote(user)
    await user.click(screen.getByRole('button', { name: 'Create quote' }))

    expect(await screen.findByText('Choose a customer')).toBeInTheDocument()
    expect(sent).toBe(false)
  })

  it('sends the term, payment frequency, deductible and start date when they are set', async () => {
    customerFound()
    let body: Record<string, unknown> = {}
    server.use(
      http.post('/api/v2/quotes', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>
        return HttpResponse.json(quote, { status: 201 })
      }),
      http.get('/api/v1/quotes/:id', () => HttpResponse.json(storedQuote)),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await pickCustomer()
    await fillQuote(user)
    await user.type(screen.getByLabelText('Policy term (years)'), '10')
    await user.selectOptions(screen.getByLabelText('Payment frequency'), 'MONTHLY')
    await user.type(screen.getByLabelText('Deductible'), '5000')
    await user.click(screen.getByRole('button', { name: 'Create quote' }))

    await waitFor(() =>
      expect(body).toMatchObject({ policyTermYears: 10, paymentFrequency: 'MONTHLY', deductible: 5000 }),
    )
    expect(body).not.toHaveProperty('startDate')
  })

  it('blocks a deductible that is not below the coverage amount', async () => {
    customerFound()
    let sent = false
    server.use(
      http.post('/api/v2/quotes', () => {
        sent = true
        return HttpResponse.json(quote, { status: 201 })
      }),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await pickCustomer()
    await fillQuote(user)
    await user.type(screen.getByLabelText('Deductible'), '250000')
    await user.click(screen.getByRole('button', { name: 'Create quote' }))

    expect(await screen.findByText('The deductible must be less than the coverage amount')).toBeInTheDocument()
    expect(sent).toBe(false)
  })

  it('selects the customer from the link on the customer page', async () => {
    customerFound()
    renderFeature(base, Component, `${base}/new?customerId=${customerId}`)

    expect(await screen.findByText('Selected customer')).toBeInTheDocument()
    expect(screen.getByText('Asha Menon')).toBeInTheDocument()
  })

  it('explains that the customer does not exist', async () => {
    customerFound()
    server.use(
      http.post('/api/v2/quotes', () =>
        HttpResponse.json({ status: 400, code: 'CUSTOMER_REFERENCE_INVALID' }, { status: 400 }),
      ),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await pickCustomer()
    await fillQuote(user)
    await user.click(screen.getByRole('button', { name: 'Create quote' }))

    expect(await screen.findByText('That customer does not exist')).toBeInTheDocument()
  })

  it.each([
    [503, 'CUSTOMER_SERVICE_UNAVAILABLE'],
    [504, 'CUSTOMER_SERVICE_TIMEOUT'],
  ])('asks the agent to retry when the customer service fails (%i)', async (status, code) => {
    customerFound()
    server.use(http.post('/api/v2/quotes', () => HttpResponse.json({ status, code }, { status })))
    renderFeature(base, Component, `${base}/new`)

    const user = await pickCustomer()
    await fillQuote(user)
    await user.click(screen.getByRole('button', { name: 'Create quote' }))

    expect(await screen.findByText(/customer service is not responding/i)).toBeInTheDocument()
  })

  it('sends a long coverage amount exactly as typed and opens the quote', async () => {
    customerFound()
    let body = ''
    server.use(
      http.post('/api/v2/quotes', async ({ request }) => {
        body = await request.text()
        return HttpResponse.json(quote, { status: 201 })
      }),
      http.get('/api/v1/quotes/:id', () => HttpResponse.json(storedQuote)),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await pickCustomer()
    await fillQuote(user, '1234567890.12')
    await user.click(screen.getByRole('button', { name: 'Create quote' }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(`${base}/q-100`))
    expect(body).toContain('"coverageAmount":1234567890.12')
  })
})

describe('quote detail page', () => {
  it('titles the quote with its number and shows the customer number', async () => {
    server.use(http.get('/api/v1/quotes/:id', () => HttpResponse.json(storedQuote)))
    renderFeature(base, Component, `${base}/q-100`)

    expect(await screen.findByRole('heading', { name: 'Quote QT-000100' })).toBeInTheDocument()
    expect(screen.getByText('CUS-000001')).toBeInTheDocument()
  })

  it('shows premiums with two decimals', async () => {
    server.use(http.get('/api/v1/quotes/:id', () => HttpResponse.json(storedQuote)))
    renderFeature(base, Component, `${base}/q-100`)

    expect(await screen.findByText('12,500.50')).toBeInTheDocument()
    expect(screen.getByText('1,041.71')).toBeInTheDocument()
  })

  const detailedQuote = {
    ...storedQuote,
    annualPremium: 12500,
    monthlyPremium: 1041.67,
    policyTermYears: 10,
    paymentFrequency: 'MONTHLY',
    deductible: 5000,
    startDate: '2026-11-01',
    validUntil: '2026-11-26',
    status: 'ISSUED',
    taxRate: 0.18,
    taxAmount: 2250,
    totalAnnualPayable: 14750,
    installmentAmount: 1229.17,
    createdAt: '2026-10-27T10:15:30.123456Z',
    createdBy: 'agent1',
  }

  it('breaks the premium down into tax, total and each instalment', async () => {
    server.use(http.get('/api/v1/quotes/:id', () => HttpResponse.json(detailedQuote)))
    renderFeature(base, Component, `${base}/q-100`)

    expect(await screen.findByText('Tax (18%)')).toBeInTheDocument()
    expect(screen.getByText('2,250.00')).toBeInTheDocument()
    expect(screen.getByText('14,750.00')).toBeInTheDocument()
    expect(screen.getByText('Monthly instalment')).toBeInTheDocument()
    expect(screen.getByText('1,229.17')).toBeInTheDocument()
  })

  it('shows the policy terms, validity and who created the quote', async () => {
    server.use(http.get('/api/v1/quotes/:id', () => HttpResponse.json(detailedQuote)))
    renderFeature(base, Component, `${base}/q-100`)

    expect(await screen.findByText('10 years')).toBeInTheDocument()
    expect(screen.getByText('5,000.00')).toBeInTheDocument()
    expect(screen.getByText('2026-11-01')).toBeInTheDocument()
    expect(screen.getByText('2026-11-26')).toBeInTheDocument()
    expect(screen.getByText('Issued')).toBeInTheDocument()
    expect(screen.getByText('agent1')).toBeInTheDocument()
  })

  it('marks an expired quote', async () => {
    server.use(http.get('/api/v1/quotes/:id', () => HttpResponse.json({ ...detailedQuote, status: 'EXPIRED' })))
    renderFeature(base, Component, `${base}/q-100`)

    expect(await screen.findByText('Expired')).toBeInTheDocument()
  })

  it('shows an older quote without the new details and without a breakdown', async () => {
    server.use(http.get('/api/v1/quotes/:id', () => HttpResponse.json(storedQuote)))
    renderFeature(base, Component, `${base}/q-100`)

    expect(await screen.findByText('12,500.50')).toBeInTheDocument()
    expect(screen.queryByText(/^Tax/)).not.toBeInTheDocument()
  })

  it('says when the quote does not exist', async () => {
    server.use(
      http.get('/api/v1/quotes/:id', () =>
        HttpResponse.json({ status: 404, code: 'QUOTE_NOT_FOUND' }, { status: 404 }),
      ),
    )
    renderFeature(base, Component, `${base}/missing`)

    expect(await screen.findByText('Quote not found')).toBeInTheDocument()
  })
})

describe('quote list page', () => {
  function page(pageNumber: number, content: unknown[], totalElements: number, totalPages: number) {
    return HttpResponse.json({ content, page: pageNumber, size: 20, totalElements, totalPages })
  }

  it('shows the total and disables Next on the last page', async () => {
    server.use(http.get('/api/v1/quotes', () => page(1, [quote], 21, 2)))
    renderFeature(base, Component, `${base}?page=1&size=20`)

    expect(await screen.findByText('21 quotes in total')).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeEnabled()
  })

  it('shows the quote number, linked to the full quote ID', async () => {
    server.use(http.get('/api/v1/quotes', () => page(0, [quote], 1, 1)))
    renderFeature(base, Component, base)

    const link = await screen.findByRole('link', { name: 'QT-000100' })
    expect(link).toHaveAttribute('href', `${base}/q-100`)
  })

  it('searches quotes by what the agent types', async () => {
    const seen: string[] = []
    server.use(
      http.get('/api/v1/quotes', ({ request }) => {
        const q = new URL(request.url).searchParams.get('q') ?? ''
        seen.push(q)
        return q === 'asha' ? page(0, [quote], 1, 1) : page(0, [], 0, 0)
      }),
    )
    renderFeature(base, Component, base)

    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Search quotes'), 'asha')

    expect(await screen.findByRole('link', { name: 'QT-000100' })).toBeInTheDocument()
    expect(seen.at(-1)).toBe('asha')
  })

  it('says so when no quote matches the search', async () => {
    server.use(http.get('/api/v1/quotes', () => page(0, [], 0, 0)))
    renderFeature(base, Component, `${base}?q=nobody`)

    expect(await screen.findByText('No quotes match your search')).toBeInTheDocument()
  })

  it('shows an empty state when there are no quotes', async () => {
    server.use(http.get('/api/v1/quotes', () => page(0, [], 0, 0)))
    renderFeature(base, Component, base)

    expect(await screen.findByText('No quotes yet')).toBeInTheDocument()
  })

  it('offers the last page when the requested page is past the end', async () => {
    server.use(http.get('/api/v1/quotes', () => page(9, [], 21, 2)))
    renderFeature(base, Component, `${base}?page=9&size=20`)

    expect(await screen.findByRole('button', { name: 'Go to the last page' })).toBeInTheDocument()
  })

  it('falls back to the default size for an unsupported size in the address', async () => {
    let query = ''
    server.use(
      http.get('/api/v1/quotes', ({ request }) => {
        query = new URL(request.url).search
        return page(0, [quote], 1, 1)
      }),
    )
    renderFeature(base, Component, `${base}?page=-3&size=5000`)

    await screen.findByText('1 quote in total')
    expect(query).toBe('?page=0&size=20')
  })
})
