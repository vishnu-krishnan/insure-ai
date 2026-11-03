import type { Customer } from '../../api/schemas'

// What a customer must have on file before they can request a quote. The profile ring on the overview uses the same list.
export function completeness(customer: Customer): number {
  const checks = [
    customer.fullName,
    customer.phone,
    customer.dateOfBirth,
    customer.gender,
    customer.maritalStatus,
    customer.occupation,
    customer.annualIncome,
    customer.address?.line1,
    customer.address?.city,
    customer.address?.state,
    customer.address?.postalCode,
    customer.address?.country,
    customer.nominee?.name,
    customer.nominee?.relationship,
    customer.idType,
    customer.idNumber,
  ]
  const filled = checks.filter((value) => value != null && String(value).trim() !== '').length
  return Math.round((filled / checks.length) * 100)
}

export function isProfileComplete(customer: Customer | null | undefined): customer is Customer {
  return customer != null && completeness(customer) === 100
}
