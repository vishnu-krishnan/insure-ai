import { describe, expect, it } from 'vitest'

import { emptyCustomerForm, myProfileSchema, splitOccupation } from './schemas'

const complete = {
  ...emptyCustomerForm,
  fullName: 'Asha Menon',
  email: 'asha@example.com',
  phone: '+919876543210',
  dateOfBirth: '1990-05-14',
  gender: 'FEMALE' as const,
  maritalStatus: 'MARRIED' as const,
  occupation: 'Salaried - Software engineer',
  idType: 'PAN' as const,
  idNumber: 'ABCDE1234F',
  annualIncome: '1200000',
  address: {
    line1: '12 Lake Road',
    line2: '',
    city: 'Pune',
    state: 'Maharashtra',
    postalCode: '411001',
    country: 'IN',
  },
  nominee: { name: 'Ravi Menon', relationship: 'Spouse' },
}

describe('my profile schema', () => {
  it('accepts a profile with every field filled in', () => {
    expect(myProfileSchema.safeParse(complete).success).toBe(true)
  })

  it('lets a profile be saved in parts', () => {
    expect(
      myProfileSchema.safeParse({
        ...emptyCustomerForm,
        fullName: 'Asha Menon',
        email: 'a@example.com',
        phone: '+919876543210',
        dateOfBirth: '1990-05-14',
      }).success,
    ).toBe(true)
  })

  it('needs the ID type and number together, and the number must fit the type', () => {
    expect(myProfileSchema.safeParse({ ...complete, idNumber: '' }).success).toBe(false)
    expect(myProfileSchema.safeParse({ ...complete, idType: '' }).success).toBe(false)
    expect(myProfileSchema.safeParse({ ...complete, idNumber: '12345' }).success).toBe(false)
    expect(myProfileSchema.safeParse({ ...complete, idType: 'AADHAAR', idNumber: '2345 6789 0123' }).success).toBe(true)
  })

  it('asks for details when the occupation needs them', () => {
    expect(myProfileSchema.safeParse({ ...complete, occupation: 'Salaried' }).success).toBe(false)
    expect(myProfileSchema.safeParse({ ...complete, occupation: 'Student' }).success).toBe(true)
  })

  it('rejects a customer outside 18 to 75', () => {
    expect(myProfileSchema.safeParse({ ...complete, dateOfBirth: '2015-01-01' }).success).toBe(false)
    expect(myProfileSchema.safeParse({ ...complete, dateOfBirth: '1900-01-01' }).success).toBe(false)
  })

  it('splits a stored occupation back into category and details', () => {
    expect(splitOccupation('Salaried - Software engineer')).toEqual({
      category: 'Salaried',
      details: 'Software engineer',
    })
    expect(splitOccupation('Plumber')).toEqual({ category: 'Other', details: 'Plumber' })
  })
})
