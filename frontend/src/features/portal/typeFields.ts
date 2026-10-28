// What an agent records for each cover type. The same definitions drive the form, its validation,
// the request body and the read-only display, so the four types cannot drift apart.
export type FieldKind = 'text' | 'int' | 'bool' | 'select'

export interface Option {
  value: string
  label: string
}

export interface FieldDef {
  key: string
  label: string
  kind: FieldKind
  options?: Option[]
  min?: number
  max?: number
  maxLength?: number
  placeholder?: string
}

const year = new Date().getFullYear()

const YES_NO: Option[] = [
  { value: 'true', label: 'Yes' },
  { value: 'false', label: 'No' },
]

function options(...values: string[]): Option[] {
  return values.map((value) => ({
    value,
    label: value.charAt(0) + value.slice(1).toLowerCase().replace('_', ' '),
  }))
}

export const TYPE_FIELDS: Record<string, FieldDef[]> = {
  AUTO: [
    { key: 'vehicleMake', label: 'Vehicle make', kind: 'text', maxLength: 60, placeholder: 'Maruti' },
    { key: 'vehicleModel', label: 'Vehicle model', kind: 'text', maxLength: 60, placeholder: 'Swift' },
    { key: 'vehicleYear', label: 'Year of manufacture', kind: 'int', min: 1980, max: year + 1, placeholder: '2020' },
    { key: 'registrationNumber', label: 'Registration number', kind: 'text', maxLength: 20, placeholder: 'KL07AB1234' },
    { key: 'usage', label: 'Usage', kind: 'select', options: options('PRIVATE', 'COMMERCIAL') },
    { key: 'previousClaims', label: 'Previous claims', kind: 'int', min: 0, max: 20, placeholder: '0' },
  ],
  HOME: [
    { key: 'propertyType', label: 'Property type', kind: 'select', options: options('APARTMENT', 'HOUSE', 'VILLA') },
    { key: 'builtYear', label: 'Year built', kind: 'int', min: 1900, max: year, placeholder: '2012' },
    { key: 'areaSqFt', label: 'Area (sq ft)', kind: 'int', min: 50, max: 100000, placeholder: '1200' },
    { key: 'ownership', label: 'Ownership', kind: 'select', options: options('OWNED', 'RENTED') },
    { key: 'city', label: 'City', kind: 'text', maxLength: 60 },
  ],
  LIFE: [
    { key: 'smoker', label: 'Smoker', kind: 'bool' },
    {
      key: 'occupationClass',
      label: 'Occupation class',
      kind: 'select',
      options: options('CLASS_1', 'CLASS_2', 'CLASS_3'),
    },
    { key: 'nomineeName', label: 'Nominee name', kind: 'text', maxLength: 80 },
    { key: 'nomineeRelationship', label: 'Nominee relationship', kind: 'text', maxLength: 40, placeholder: 'Spouse' },
  ],
  HEALTH: [
    { key: 'members', label: 'Members covered', kind: 'int', min: 1, max: 10, placeholder: '4' },
    { key: 'preExistingConditions', label: 'Pre-existing conditions', kind: 'bool' },
    { key: 'smoker', label: 'Smoker', kind: 'bool' },
    { key: 'planTier', label: 'Plan tier', kind: 'select', options: options('BASIC', 'STANDARD', 'PREMIUM') },
  ],
}

export type DetailValues = Record<string, string>

export function emptyDetails(type: string): DetailValues {
  return Object.fromEntries((TYPE_FIELDS[type] ?? []).map((field) => [field.key, '']))
}

function choices(field: FieldDef): Option[] {
  return field.kind === 'bool' ? YES_NO : (field.options ?? [])
}

export function validateDetails(type: string, values: DetailValues): Record<string, string> {
  const errors: Record<string, string> = {}
  for (const field of TYPE_FIELDS[type] ?? []) {
    const value = (values[field.key] ?? '').trim()
    if (field.kind === 'text') {
      if (value === '') errors[field.key] = 'Required'
      else if (field.maxLength && value.length > field.maxLength)
        errors[field.key] = `At most ${field.maxLength} characters`
    } else if (field.kind === 'int') {
      const number = Number(value)
      if (!/^\d+$/.test(value) || number < (field.min ?? 0) || number > (field.max ?? Number.MAX_SAFE_INTEGER)) {
        errors[field.key] = `Enter a whole number from ${field.min} to ${field.max}`
      }
    } else if (!choices(field).some((option) => option.value === value)) {
      errors[field.key] = 'Choose an option'
    }
  }
  return errors
}

export function toDetailsPayload(type: string, values: DetailValues): Record<string, string | number | boolean> {
  const payload: Record<string, string | number | boolean> = {}
  for (const field of TYPE_FIELDS[type] ?? []) {
    const value = (values[field.key] ?? '').trim()
    payload[field.key] = field.kind === 'int' ? Number(value) : field.kind === 'bool' ? value === 'true' : value
  }
  return payload
}

export function describeDetails(type: string, details: Record<string, unknown> | null | undefined) {
  if (!details) return []
  return (TYPE_FIELDS[type] ?? [])
    .filter((field) => details[field.key] !== undefined && details[field.key] !== null)
    .map((field) => {
      const raw = details[field.key]
      let value = String(raw)
      if (field.kind === 'bool') value = raw === true ? 'Yes' : 'No'
      else if (field.kind === 'select') value = field.options?.find((option) => option.value === raw)?.label ?? value
      return { label: field.label, value }
    })
}

export function fieldChoices(field: FieldDef): Option[] {
  return choices(field)
}
