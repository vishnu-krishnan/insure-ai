import { getCountries, getCountryCallingCode, parsePhoneNumberFromString, type CountryCode } from 'libphonenumber-js'

export interface Country {
  code: CountryCode
  name: string
  dial: string
}

const names = new Intl.DisplayNames(['en'], { type: 'region' })

export const COUNTRIES: Country[] = getCountries()
  .map((code) => ({ code, name: names.of(code) ?? code, dial: getCountryCallingCode(code) }))
  .sort((a, b) => a.name.localeCompare(b.name))

export function findCountry(code: string): Country | undefined {
  return COUNTRIES.find((country) => country.code === code.toUpperCase())
}

export const DEFAULT_COUNTRY: CountryCode = 'IN'

// Splits a stored international number into its country and the digits after the dial code.
export function splitPhone(value: string): { country: CountryCode; national: string } | null {
  const parsed = parsePhoneNumberFromString(value)
  if (!parsed?.country) {
    return null
  }
  return { country: parsed.country, national: parsed.nationalNumber }
}

export function joinPhone(country: string, national: string): string {
  const digits = national.replace(/\D/g, '')
  const dial = findCountry(country)?.dial
  return digits === '' || !dial ? '' : `+${dial}${digits}`
}
