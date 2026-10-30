import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { describe, expect, it } from 'vitest'

import { renderFeature } from '../../test/renderFeature'
import { server } from '../../test/server'
import { Component } from './routes'

const customer = {
  customerId: '10000001',
  customerNumber: 'CUS-000001',
  fullName: 'Asha Menon',
  email: 'asha@example.com',
  phone: '+919876543210',
  dateOfBirth: '1990-04-12',
}

const base = '/agent/customers'

async function fillForm(overrides: Partial<typeof customer> = {}) {
  const user = userEvent.setup()
  const values = { ...customer, ...overrides }
  await user.type(screen.getByLabelText('Full name'), values.fullName)
  await user.type(screen.getByLabelText('Email'), values.email)
  await user.type(screen.getByLabelText('Phone'), values.phone)
  await user.type(screen.getByLabelText('Date of birth'), values.dateOfBirth)
  return user
}

describe('customer create page', () => {
  it('builds the international number from the chosen country and the local number', async () => {
    let body: Record<string, unknown> = {}
    server.use(
      http.post('/api/v1/customers', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>
        return HttpResponse.json(customer, { status: 201 })
      }),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Full name'), 'Asha Menon')
    await user.type(screen.getByLabelText('Email'), 'asha@example.com')
    await user.click(screen.getByRole('combobox', { name: 'Phone country' }))
    await user.click(await screen.findByText('United Kingdom (+44)'))
    await user.type(screen.getByLabelText('Phone'), '7911123456')
    await user.type(screen.getByLabelText('Date of birth'), '1990-05-14')
    await user.click(screen.getByRole('button', { name: 'Create customer' }))

    await waitFor(() => expect(body).toMatchObject({ phone: '+447911123456' }))
  })

  it('rejects a phone number that is not international before sending anything', async () => {
    let sent = false
    server.use(
      http.post('/api/v1/customers', () => {
        sent = true
        return HttpResponse.json(customer, { status: 201 })
      }),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await fillForm({ phone: '98765' })
    await user.click(screen.getByRole('button', { name: 'Create customer' }))

    expect(await screen.findByText(/international format/i)).toBeInTheDocument()
    expect(sent).toBe(false)
  })

  it('shows a server field error under the matching input', async () => {
    server.use(
      http.post('/api/v1/customers', () =>
        HttpResponse.json(
          {
            status: 400,
            code: 'VALIDATION_FAILED',
            errors: [{ field: 'email', message: 'email is already registered' }],
          },
          { status: 400 },
        ),
      ),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await fillForm()
    await user.click(screen.getByRole('button', { name: 'Create customer' }))

    expect(await screen.findByText('email is already registered')).toBeInTheDocument()
  })

  it('sends one request when the button is clicked twice', async () => {
    let calls = 0
    server.use(
      http.post('/api/v1/customers', async () => {
        calls += 1
        await delay(80)
        return HttpResponse.json(customer, { status: 201 })
      }),
      http.get('/api/v1/customers/:id', () => HttpResponse.json(customer)),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await fillForm()
    await user.dblClick(screen.getByRole('button', { name: 'Create customer' }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(`${base}/${customer.customerId}`))
    expect(calls).toBe(1)
  })

  it('sends the optional details the agent filled in', async () => {
    let body: Record<string, unknown> = {}
    server.use(
      http.post('/api/v1/customers', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>
        return HttpResponse.json(customer, { status: 201 })
      }),
      http.get('/api/v1/customers/:id', () => HttpResponse.json(customer)),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await fillForm()
    await user.selectOptions(screen.getByLabelText('Gender'), 'FEMALE')
    await user.selectOptions(screen.getByLabelText('Marital status'), 'MARRIED')
    await user.type(screen.getByLabelText('Occupation'), 'Engineer')
    await user.type(screen.getByLabelText('Annual income'), '1200000')
    await user.type(screen.getByLabelText('Address line 1'), '12 Lake Road')
    await user.type(screen.getByLabelText('City'), 'Pune')
    await user.type(screen.getByLabelText('State'), 'Maharashtra')
    await user.type(screen.getByLabelText('Postal code'), '411001')
    await user.click(screen.getByRole('combobox', { name: 'Country' }))
    await user.click(await screen.findByText('India'))
    await user.type(screen.getByLabelText('Nominee name'), 'Ravi Menon')
    await user.type(screen.getByLabelText('Relationship'), 'Spouse')
    await user.click(screen.getByRole('button', { name: 'Create customer' }))

    await waitFor(() => expect(body).toMatchObject({ gender: 'FEMALE', annualIncome: 1200000 }))
    expect(body).toMatchObject({
      maritalStatus: 'MARRIED',
      occupation: 'Engineer',
      address: { line1: '12 Lake Road', city: 'Pune', state: 'Maharashtra', postalCode: '411001', country: 'IN' },
      nominee: { name: 'Ravi Menon', relationship: 'Spouse' },
    })
  })

  it('sends only the required fields when the optional sections are left empty', async () => {
    let body: Record<string, unknown> = {}
    server.use(
      http.post('/api/v1/customers', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>
        return HttpResponse.json(customer, { status: 201 })
      }),
      http.get('/api/v1/customers/:id', () => HttpResponse.json(customer)),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await fillForm()
    await user.click(screen.getByRole('button', { name: 'Create customer' }))

    await waitFor(() => expect(Object.keys(body).sort()).toEqual(['dateOfBirth', 'email', 'fullName', 'phone']))
  })

  it('asks for the rest of the address before sending a half-filled one', async () => {
    let sent = false
    server.use(
      http.post('/api/v1/customers', () => {
        sent = true
        return HttpResponse.json(customer, { status: 201 })
      }),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await fillForm()
    await user.type(screen.getByLabelText('City'), 'Pune')
    await user.click(screen.getByRole('button', { name: 'Create customer' }))

    expect(await screen.findByText('Enter the first address line')).toBeInTheDocument()
    expect(screen.getByText('Enter the postal code')).toBeInTheDocument()
    expect(sent).toBe(false)
  })

  it('shows a server error for a nested field under that field', async () => {
    server.use(
      http.post('/api/v1/customers', () =>
        HttpResponse.json(
          {
            status: 400,
            code: 'VALIDATION_FAILED',
            errors: [{ field: 'address.postalCode', message: 'address.postalCode must be 3 to 12 letters' }],
          },
          { status: 400 },
        ),
      ),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await fillForm()
    await user.type(screen.getByLabelText('Address line 1'), '12 Lake Road')
    await user.type(screen.getByLabelText('City'), 'Pune')
    await user.type(screen.getByLabelText('State'), 'Maharashtra')
    await user.type(screen.getByLabelText('Postal code'), '411001')
    await user.click(screen.getByRole('combobox', { name: 'Country' }))
    await user.click(await screen.findByText('India'))
    await user.click(screen.getByRole('button', { name: 'Create customer' }))

    expect(await screen.findAllByText('address.postalCode must be 3 to 12 letters')).toHaveLength(1)
  })

  it('opens the new customer after a successful create', async () => {
    server.use(
      http.post('/api/v1/customers', () => HttpResponse.json(customer, { status: 201 })),
      http.get('/api/v1/customers/:id', () => HttpResponse.json(customer)),
    )
    renderFeature(base, Component, `${base}/new`)

    const user = await fillForm()
    await user.click(screen.getByRole('button', { name: 'Create customer' }))

    expect(await screen.findByText('Asha Menon')).toBeInTheDocument()
    expect(screen.getByText('asha@example.com')).toBeInTheDocument()
    expect(screen.getByText('CUS-000001')).toBeInTheDocument()
  })
})

describe('customer detail page', () => {
  const full = {
    ...customer,
    gender: 'FEMALE',
    maritalStatus: 'MARRIED',
    occupation: 'Engineer',
    annualIncome: 1200000.5,
    address: {
      line1: '12 Lake Road',
      line2: 'Baner',
      city: 'Pune',
      state: 'Maharashtra',
      postalCode: '411001',
      country: 'IN',
    },
    nominee: { name: 'Ravi Menon', relationship: 'Spouse' },
    createdAt: '2026-10-27T10:15:30.123456Z',
    updatedAt: '2026-10-27T10:15:30.123456Z',
  }
  const quotePage = (content: unknown[]) =>
    HttpResponse.json({ content, page: 0, size: 5, totalElements: content.length, totalPages: 1 })

  it('shows personal details, address and nominee', async () => {
    server.use(
      http.get('/api/v1/customers/:id', () => HttpResponse.json(full)),
      http.get('/api/v1/quotes', () => quotePage([])),
    )
    renderFeature(base, Component, `${base}/${customer.customerId}`)

    expect(await screen.findByText('Engineer')).toBeInTheDocument()
    expect(screen.getByText('Female')).toBeInTheDocument()
    expect(screen.getByText('Married')).toBeInTheDocument()
    expect(screen.getByText('1,200,000.50')).toBeInTheDocument()
    expect(screen.getByText(/12 Lake Road/)).toBeInTheDocument()
    expect(screen.getByText(/Pune, Maharashtra 411001/)).toBeInTheDocument()
    expect(screen.getByText('Ravi Menon')).toBeInTheDocument()
    expect(screen.getByText('Spouse')).toBeInTheDocument()
  })

  it('says a detail was not recorded for a customer created before details existed', async () => {
    server.use(
      http.get('/api/v1/customers/:id', () => HttpResponse.json(customer)),
      http.get('/api/v1/quotes', () => quotePage([])),
    )
    renderFeature(base, Component, `${base}/${customer.customerId}`)

    expect(await screen.findByText('No address recorded')).toBeInTheDocument()
    expect(screen.getByText('No nominee recorded')).toBeInTheDocument()
  })

  it('lists the quotes for this customer, linked to each quote', async () => {
    let asked = ''
    server.use(
      http.get('/api/v1/customers/:id', () => HttpResponse.json(full)),
      http.get('/api/v1/quotes', ({ request }) => {
        asked = new URL(request.url).searchParams.get('q') ?? ''
        return quotePage([
          {
            quoteId: 'q-100',
            quoteNumber: 'QT-000100',
            customerNumber: 'CUS-000001',
            customerName: 'Asha Menon',
            coverageType: 'HOME',
            age: 36,
            coverageAmount: 250000,
            annualPremium: 12500.5,
            monthlyPremium: 1041.71,
          },
        ])
      }),
    )
    renderFeature(base, Component, `${base}/${customer.customerId}`)

    const link = await screen.findByRole('link', { name: 'QT-000100' })
    expect(link).toHaveAttribute('href', '/agent/quotes/q-100')
    expect(asked).toBe('CUS-000001')
  })

  it('still shows the customer when the quotes cannot be loaded', async () => {
    server.use(
      http.get('/api/v1/customers/:id', () => HttpResponse.json(full)),
      http.get('/api/v1/quotes', () => HttpResponse.json({ status: 503, code: 'X' }, { status: 503 })),
    )
    renderFeature(base, Component, `${base}/${customer.customerId}`)

    expect(await screen.findByText('Engineer')).toBeInTheDocument()
    expect(await screen.findByText('Quotes could not be loaded')).toBeInTheDocument()
  })
})

describe('customer search', () => {
  function results(content: unknown[], totalElements = content.length, totalPages = content.length ? 1 : 0) {
    return HttpResponse.json({ content, page: 0, size: 20, totalElements, totalPages })
  }

  it('lists customers with their business ID, linked to the customer', async () => {
    server.use(http.get('/api/v1/customers', () => results([customer])))
    renderFeature(base, Component, base)

    const link = await screen.findByRole('link', { name: 'CUS-000001' })
    expect(link).toHaveAttribute('href', `${base}/${customer.customerId}`)
    expect(screen.getByText('Asha Menon')).toBeInTheDocument()
    expect(screen.getByText('asha@example.com')).toBeInTheDocument()
  })

  it('searches by what the agent types', async () => {
    const seen: string[] = []
    server.use(
      http.get('/api/v1/customers', ({ request }) => {
        const q = new URL(request.url).searchParams.get('q') ?? ''
        seen.push(q)
        return results(q === 'asha' ? [customer] : [])
      }),
    )
    renderFeature(base, Component, base)

    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Search customers'), 'asha')

    expect(await screen.findByRole('link', { name: 'CUS-000001' })).toBeInTheDocument()
    expect(seen.at(-1)).toBe('asha')
  })

  it('says so when nothing matches', async () => {
    server.use(http.get('/api/v1/customers', () => results([])))
    renderFeature(base, Component, `${base}?q=nobody`)

    expect(await screen.findByText('No customers match your search')).toBeInTheDocument()
  })

  it('shows the reference when the customer service fails', async () => {
    server.use(
      http.get('/api/v1/customers', () =>
        HttpResponse.json({ status: 500, code: 'INTERNAL_ERROR', correlationId: 'abc-123' }, { status: 500 }),
      ),
    )
    renderFeature(base, Component, base)

    expect(await screen.findByText('Reference: abc-123')).toBeInTheDocument()
  })

  it('asks for the next page', async () => {
    const pages: string[] = []
    server.use(
      http.get('/api/v1/customers', ({ request }) => {
        pages.push(new URL(request.url).searchParams.get('page') ?? '')
        return results([customer], 21, 2)
      }),
    )
    renderFeature(base, Component, base)

    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Next page' }))

    await waitFor(() => expect(pages).toContain('1'))
  })
})
