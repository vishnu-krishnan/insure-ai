import { describe, expect, it } from 'vitest'

import { TYPE_FIELDS, describeDetails, emptyDetails, toDetailsPayload, validateDetails } from './typeFields'

const year = new Date().getFullYear()

describe('type fields', () => {
  it('defines fields for every cover type', () => {
    expect(Object.keys(TYPE_FIELDS).sort()).toEqual(['AUTO', 'HEALTH', 'HOME', 'LIFE'])
  })

  it('asks for every field when nothing is filled in', () => {
    const errors = validateDetails('AUTO', emptyDetails('AUTO'))
    expect(Object.keys(errors).sort()).toEqual(TYPE_FIELDS.AUTO.map((field) => field.key).sort())
  })

  it('accepts a complete auto form and sends typed values', () => {
    const values = {
      vehicleMake: 'Maruti',
      vehicleModel: 'Swift',
      vehicleYear: '2020',
      registrationNumber: 'KL07AB1234',
      usage: 'PRIVATE',
      previousClaims: '0',
    }
    expect(validateDetails('AUTO', values)).toEqual({})
    expect(toDetailsPayload('AUTO', values)).toEqual({
      vehicleMake: 'Maruti',
      vehicleModel: 'Swift',
      vehicleYear: 2020,
      registrationNumber: 'KL07AB1234',
      usage: 'PRIVATE',
      previousClaims: 0,
    })
  })

  it('rejects numbers outside the allowed range', () => {
    const values = { ...emptyDetails('HOME'), builtYear: String(year + 1), areaSqFt: '10' }
    const errors = validateDetails('HOME', values)
    expect(errors.builtYear).toBeDefined()
    expect(errors.areaSqFt).toBeDefined()
  })

  it('sends yes and no choices as booleans', () => {
    const values = {
      smoker: 'true',
      occupationClass: 'CLASS_1',
      nomineeName: 'Asha',
      nomineeRelationship: 'Spouse',
    }
    expect(validateDetails('LIFE', values)).toEqual({})
    expect(toDetailsPayload('LIFE', values).smoker).toBe(true)
  })

  it('describes saved details with readable labels and skips unknown keys', () => {
    const rows = describeDetails('HEALTH', {
      members: 3,
      preExistingConditions: false,
      smoker: true,
      planTier: 'BASIC',
      other: 1,
    })
    expect(rows).toEqual([
      { label: 'Members covered', value: '3' },
      { label: 'Pre-existing conditions', value: 'No' },
      { label: 'Smoker', value: 'Yes' },
      { label: 'Plan tier', value: 'Basic' },
    ])
  })

  it('returns no rows when nothing was recorded', () => {
    expect(describeDetails('AUTO', null)).toEqual([])
  })
})
