import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { server } from '../test/server'
import { createCustomer, getCustomer, searchCustomers } from './customers'
import { customerRequestSchema, emptyCustomerForm, toCustomerPayload } from './schemas'

const customer = {
  customerId: '10000001',
  customerNumber: 'CUS-000001',
  fullName: 'Asha Menon',
  email: 'asha@example.com',
  phone: '+919876543210',
  dateOfBirth: '1990-04-12',
}

const form = {
  ...emptyCustomerForm,
  fullName: customer.fullName,
  email: customer.email,
  phone: customer.phone,
  dateOfBirth: customer.dateOfBirth,
}
const address = {
  line1: '12 Lake Road',
  line2: '',
  city: 'Pune',
  state: 'Maharashtra',
  postalCode: '411001',
  country: 'in',
}

function problems(values: unknown): string[] {
  const result = customerRequestSchema.safeParse(values)
  return result.success ? [] : result.error.issues.map((issue) => issue.path.join('.'))
}

describe('customer schema', () => {
  it('rejects a phone number that is not E.164', () => {
    expect(problems({ ...form, phone: '98765' })).toContain('phone')
  })

  it('rejects a date of birth in the future', () => {
    expect(problems({ ...form, dateOfBirth: '2999-01-01' })).toContain('dateOfBirth')
  })

  it('accepts a customer with only the required fields', () => {
    expect(problems(form)).toEqual([])
  })

  it('rejects an address that is only partly filled in', () => {
    expect(problems({ ...form, address: { ...emptyCustomerForm.address, city: 'Pune' } })).toEqual(
      expect.arrayContaining(['address.line1', 'address.state', 'address.postalCode', 'address.country']),
    )
  })

  it('rejects a country that is not a two-letter code', () => {
    expect(problems({ ...form, address: { ...address, country: 'India' } })).toContain('address.country')
  })

  it('rejects a nominee with a name but no relationship', () => {
    expect(problems({ ...form, nominee: { name: 'Ravi Menon', relationship: '' } })).toContain('nominee.relationship')
  })

  it('rejects a negative or over-precise annual income', () => {
    expect(problems({ ...form, annualIncome: '-5' })).toContain('annualIncome')
    expect(problems({ ...form, annualIncome: '10.123' })).toContain('annualIncome')
  })
})

describe('customer payload', () => {
  it('leaves every optional field out when the form left it empty', () => {
    expect(toCustomerPayload(form)).toEqual({
      fullName: customer.fullName,
      email: customer.email,
      phone: customer.phone,
      dateOfBirth: customer.dateOfBirth,
    })
  })

  it('sends the details that were filled in, with the income as a number', () => {
    const payload = toCustomerPayload({
      ...form,
      gender: 'FEMALE',
      maritalStatus: 'MARRIED',
      occupation: ' Engineer ',
      annualIncome: '1200000.50',
      address,
      nominee: { name: 'Ravi Menon', relationship: 'Spouse' },
    })

    expect(payload).toMatchObject({
      gender: 'FEMALE',
      maritalStatus: 'MARRIED',
      occupation: 'Engineer',
      annualIncome: 1200000.5,
      address: { line1: '12 Lake Road', city: 'Pune', postalCode: '411001', country: 'IN' },
      nominee: { name: 'Ravi Menon', relationship: 'Spouse' },
    })
    expect(payload.address).not.toHaveProperty('line2')
  })
})

describe('customer api', () => {
  it('creates a customer', async () => {
    server.use(http.post('/api/v1/customers', () => HttpResponse.json(customer, { status: 201 })))

    const created = await createCustomer({
      fullName: customer.fullName,
      email: customer.email,
      phone: customer.phone,
      dateOfBirth: customer.dateOfBirth,
    })

    expect(created.customerId).toBe(customer.customerId)
  })

  it('reports an unknown customer as CUSTOMER_NOT_FOUND', async () => {
    server.use(
      http.get('/api/v1/customers/:id', () =>
        HttpResponse.json({ status: 404, code: 'CUSTOMER_NOT_FOUND' }, { status: 404 }),
      ),
    )

    await expect(getCustomer(customer.customerId)).rejects.toMatchObject({
      status: 404,
      code: 'CUSTOMER_NOT_FOUND',
    })
  })
})

describe('customer search', () => {
  function respondWith(onRequest: (search: string) => void) {
    server.use(
      http.get('/api/v1/customers', ({ request }) => {
        onRequest(new URL(request.url).search)
        return HttpResponse.json({ content: [customer], page: 0, size: 20, totalElements: 1, totalPages: 1 })
      }),
    )
  }

  it('sends the search text, page and size and reads the business number', async () => {
    let search = ''
    respondWith((value) => (search = value))

    const page = await searchCustomers('asha m', 0, 20)

    expect(search).toBe('?q=asha+m&page=0&size=20')
    expect(page.content[0]?.customerNumber).toBe('CUS-000001')
  })

  it('leaves the search text out when it is blank', async () => {
    let search = ''
    respondWith((value) => (search = value))

    await searchCustomers('   ', 2, 10)

    expect(search).toBe('?page=2&size=10')
  })
})
