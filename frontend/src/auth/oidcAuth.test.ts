import type { User } from 'oidc-client-ts'
import { describe, expect, it } from 'vitest'

import { toAuthUser } from './oidcAuth'

function token(claims: object): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '')
  return `${encode({ alg: 'none' })}.${encode(claims)}.`
}

function userWith(profile: object): User {
  return { profile, access_token: token({ realm_access: { roles: ['customer'] } }) } as unknown as User
}

describe('toAuthUser', () => {
  it('takes the full name from the sign-up details', () => {
    const user = toAuthUser(userWith({ preferred_username: 'vishnu123', name: 'Vishnu Krishnan' }))
    expect(user).toMatchObject({ name: 'vishnu123', fullName: 'Vishnu Krishnan', roles: ['customer'] })
  })

  it('builds it from the first and last name when there is no name claim', () => {
    const user = toAuthUser(userWith({ preferred_username: 'a', given_name: 'Vishnu', family_name: 'Krishnan' }))
    expect(user.fullName).toBe('Vishnu Krishnan')
  })

  it('has no full name when the account has none', () => {
    expect(toAuthUser(userWith({ preferred_username: 'a' })).fullName).toBeUndefined()
  })
})
