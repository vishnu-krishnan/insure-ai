import { getConfig } from '../config'
import { request } from './http'
import {
  customerPageSchema,
  customerResponseSchema,
  type Customer,
  type CustomerPage,
  type CustomerPayload,
} from './schemas'

export function createCustomer(input: CustomerPayload): Promise<Customer> {
  return request(getConfig().apiBase.customers, '/api/v1/customers', customerResponseSchema, {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function getCustomer(customerId: string): Promise<Customer> {
  return request(
    getConfig().apiBase.customers,
    `/api/v1/customers/${encodeURIComponent(customerId)}`,
    customerResponseSchema,
  )
}

export function searchCustomers(text: string, page: number, size: number): Promise<CustomerPage> {
  const params = new URLSearchParams()
  if (text.trim()) {
    params.set('q', text.trim())
  }
  params.set('page', String(page))
  params.set('size', String(size))
  return request(getConfig().apiBase.customers, `/api/v1/customers?${params}`, customerPageSchema)
}
