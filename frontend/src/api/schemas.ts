import { z } from 'zod'

import { validateDetails } from '../features/portal/typeFields'

export const COVERAGE_TYPES = ['AUTO', 'HOME', 'LIFE', 'HEALTH'] as const

const isoDate = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Enter a date')

const amount = z
  .string()
  .trim()
  .regex(/^\d{1,15}(\.\d{1,2})?$/, 'Enter an amount with at most two decimals')
  .refine((value) => Number(value) > 0, 'Amount must be greater than zero')

export const GENDERS = ['MALE', 'FEMALE', 'OTHER', 'UNSPECIFIED'] as const
export const MARITAL_STATUSES = ['SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED'] as const

const emptyAddress = { line1: '', line2: '', city: '', state: '', postalCode: '', country: '' }
const emptyNominee = { name: '', relationship: '' }

export const customerRequestSchema = z
  .object({
    fullName: z
      .string()
      .trim()
      .min(2, 'Name must be at least 2 characters')
      .max(80, 'Name must be at most 80 characters'),
    email: z
      .string()
      .trim()
      .max(254, 'Email must be at most 254 characters')
      .pipe(z.email('Enter a valid email address')),
    phone: z
      .string()
      .trim()
      .regex(/^\+[1-9][0-9]{7,14}$/, 'Use international format, for example +919876543210'),
    dateOfBirth: isoDate.refine(
      (value) => value < new Date().toISOString().slice(0, 10),
      'Date of birth must be in the past',
    ),
    gender: z.enum(['', ...GENDERS]),
    maritalStatus: z.enum(['', ...MARITAL_STATUSES]),
    occupation: z.string().trim().max(80, 'Occupation must be at most 80 characters'),
    annualIncome: z
      .string()
      .trim()
      .regex(/^(\d{1,12}(\.\d{1,2})?)?$/, 'Enter an amount of zero or more with at most two decimals'),
    address: z.object({
      line1: z.string().trim().max(120, 'Address line must be at most 120 characters'),
      line2: z.string().trim().max(120, 'Address line must be at most 120 characters'),
      city: z.string().trim().max(80, 'City must be at most 80 characters'),
      state: z.string().trim().max(80, 'State must be at most 80 characters'),
      postalCode: z.string().trim(),
      country: z.string().trim(),
    }),
    nominee: z.object({
      name: z.string().trim(),
      relationship: z.string().trim().max(40, 'Relationship must be at most 40 characters'),
    }),
  })
  .superRefine((value, context) => {
    const { line2: _optional, ...required } = value.address
    const started = Object.values(required).some((part) => part !== '')
    if (started) {
      const messages: Record<string, string> = {
        line1: 'Enter the first address line',
        city: 'Enter the city',
        state: 'Enter the state',
        postalCode: 'Enter the postal code',
        country: 'Enter the country',
      }
      for (const [field, message] of Object.entries(messages)) {
        if (required[field as keyof typeof required] === '') {
          context.addIssue({ code: 'custom', path: ['address', field], message })
        }
      }
      if (
        value.address.postalCode !== '' &&
        !/^[A-Za-z0-9-][A-Za-z0-9 -]{1,10}[A-Za-z0-9-]$/.test(value.address.postalCode)
      ) {
        context.addIssue({
          code: 'custom',
          path: ['address', 'postalCode'],
          message: 'Postal code must be 3 to 12 letters, digits, spaces or hyphens',
        })
      }
      if (value.address.country !== '' && !/^[A-Za-z]{2}$/.test(value.address.country)) {
        context.addIssue({
          code: 'custom',
          path: ['address', 'country'],
          message: 'Use a two-letter country code, for example IN',
        })
      }
    }
    if (value.nominee.name !== '' || value.nominee.relationship !== '') {
      if (value.nominee.name.length < 2 || value.nominee.name.length > 80) {
        context.addIssue({
          code: 'custom',
          path: ['nominee', 'name'],
          message: 'Nominee name must be 2 to 80 characters',
        })
      }
      if (value.nominee.relationship === '') {
        context.addIssue({ code: 'custom', path: ['nominee', 'relationship'], message: 'Enter the relationship' })
      }
    }
  })
export type CustomerRequest = z.infer<typeof customerRequestSchema>

export const emptyCustomerForm: CustomerRequest = {
  fullName: '',
  email: '',
  phone: '',
  dateOfBirth: '',
  gender: '',
  maritalStatus: '',
  occupation: '',
  annualIncome: '',
  address: emptyAddress,
  nominee: emptyNominee,
}

export interface CustomerPayload {
  fullName: string
  email: string
  phone: string
  dateOfBirth: string
  gender?: string
  maritalStatus?: string
  occupation?: string
  annualIncome?: number
  address?: { line1: string; line2?: string; city: string; state: string; postalCode: string; country: string }
  nominee?: { name: string; relationship: string }
}

export function toCustomerPayload(form: CustomerRequest): CustomerPayload {
  const payload: CustomerPayload = {
    fullName: form.fullName.trim(),
    email: form.email.trim(),
    phone: form.phone.trim(),
    dateOfBirth: form.dateOfBirth,
  }
  if (form.gender) payload.gender = form.gender
  if (form.maritalStatus) payload.maritalStatus = form.maritalStatus
  if (form.occupation.trim()) payload.occupation = form.occupation.trim()
  if (form.annualIncome.trim()) payload.annualIncome = Number(form.annualIncome)
  const { line2, ...address } = form.address
  if (Object.values(address).some((part) => part.trim() !== '')) {
    payload.address = {
      ...address,
      ...(line2.trim() ? { line2: line2.trim() } : {}),
      country: address.country.trim().toUpperCase(),
    }
  }
  if (form.nominee.name.trim() || form.nominee.relationship.trim()) {
    payload.nominee = { name: form.nominee.name.trim(), relationship: form.nominee.relationship.trim() }
  }
  return payload
}

export const customerResponseSchema = z.object({
  customerId: z.string(),
  customerNumber: z.string(),
  fullName: z.string(),
  email: z.string(),
  phone: z.string(),
  dateOfBirth: isoDate,
  gender: z.string().nullish(),
  maritalStatus: z.string().nullish(),
  occupation: z.string().nullish(),
  annualIncome: z.number().nullish(),
  address: z
    .object({
      line1: z.string(),
      line2: z.string().nullish(),
      city: z.string(),
      state: z.string(),
      postalCode: z.string(),
      country: z.string(),
    })
    .nullish(),
  nominee: z.object({ name: z.string(), relationship: z.string() }).nullish(),
  createdAt: z.string().nullish(),
  updatedAt: z.string().nullish(),
})
export type Customer = z.infer<typeof customerResponseSchema>

export const customerPageSchema = z.object({
  content: z.array(customerResponseSchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
})
export type CustomerPage = z.infer<typeof customerPageSchema>

export const PAYMENT_FREQUENCIES = ['MONTHLY', 'QUARTERLY', 'YEARLY'] as const

const dayOffset = (days: number) => new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10)

export const customerQuoteRequestSchema = z
  .object({
    customerId: z.uuid('Choose a customer'),
    coverageType: z.enum(COVERAGE_TYPES, 'Choose a coverage type'),
    coverageAmount: amount,
    policyTermYears: z
      .string()
      .trim()
      .refine((value) => value === '' || (/^\d+$/.test(value) && Number(value) >= 1 && Number(value) <= 40), {
        message: 'Enter a whole number of years from 1 to 40',
      }),
    paymentFrequency: z.enum(['', ...PAYMENT_FREQUENCIES], 'Choose how often to pay'),
    deductible: z
      .string()
      .trim()
      .regex(/^(\d{1,12}(\.\d{1,2})?)?$/, 'Enter an amount of zero or more with at most two decimals'),
    details: z.record(z.string(), z.string()),
    startDate: z
      .string()
      .refine(
        (value) =>
          value === '' || (/^\d{4}-\d{2}-\d{2}$/.test(value) && value >= dayOffset(0) && value <= dayOffset(365)),
        {
          message: 'Choose a date from today up to one year ahead',
        },
      ),
  })
  .superRefine((value, context) => {
    if (Object.keys(value.details).length > 0) {
      for (const [key, message] of Object.entries(validateDetails(value.coverageType, value.details))) {
        context.addIssue({ code: 'custom', path: ['details', key], message })
      }
    }
    if (
      value.deductible !== '' &&
      /^\d+(\.\d+)?$/.test(value.coverageAmount) &&
      Number(value.deductible) >= Number(value.coverageAmount)
    ) {
      context.addIssue({
        code: 'custom',
        path: ['deductible'],
        message: 'The deductible must be less than the coverage amount',
      })
    }
  })
export type CustomerQuoteRequest = z.infer<typeof customerQuoteRequestSchema>

export const emptyQuoteForm: CustomerQuoteRequest = {
  customerId: '',
  coverageType: 'AUTO',
  coverageAmount: '',
  policyTermYears: '',
  paymentFrequency: '',
  deductible: '',
  startDate: '',
  details: {},
}

const premiums = {
  coverageAmount: z.number(),
  annualPremium: z.number(),
  monthlyPremium: z.number(),
  policyTermYears: z.number().nullish(),
  paymentFrequency: z.string().nullish(),
  deductible: z.number().nullish(),
  startDate: z.string().nullish(),
  details: z.record(z.string(), z.unknown()).nullish(),
  validUntil: z.string().nullish(),
  status: z.string().nullish(),
  taxRate: z.number().nullish(),
  taxAmount: z.number().nullish(),
  totalAnnualPayable: z.number().nullish(),
  installmentAmount: z.number().nullish(),
  createdAt: z.string().nullish(),
  createdBy: z.string().nullish(),
}

export const customerQuoteResponseSchema = z.object({
  quoteId: z.string(),
  quoteNumber: z.string(),
  customerId: z.string(),
  customerNumber: z.string().nullish(),
  customerName: z.string(),
  coverageType: z.string(),
  age: z.number(),
  ...premiums,
})
export type CustomerQuote = z.infer<typeof customerQuoteResponseSchema>

export const quoteSummarySchema = z.object({
  quoteId: z.string(),
  quoteNumber: z.string(),
  customerNumber: z.string().nullish(),
  customerName: z.string(),
  coverageType: z.string(),
  age: z.number(),
  ...premiums,
})
export type QuoteSummary = z.infer<typeof quoteSummarySchema>

export const quotePageSchema = z.object({
  content: z.array(quoteSummarySchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
})
export type QuotePage = z.infer<typeof quotePageSchema>
