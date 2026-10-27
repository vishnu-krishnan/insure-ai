import { describe, expect, it } from 'vitest'

import { fakeToken } from '../test/tokens'
import { rolesFromToken } from './roles'

describe('rolesFromToken', () => {
  it('reads realm roles from the access token', () => {
    expect(rolesFromToken(fakeToken(['agent', 'offline_access']))).toEqual(['agent', 'offline_access'])
  })

  it('returns no roles for a malformed token', () => {
    expect(rolesFromToken('not-a-token')).toEqual([])
  })

  it('returns no roles when the claim is missing', () => {
    const noClaim = `${btoa('{}')}.${btoa('{"sub":"1"}')}.sig`

    expect(rolesFromToken(noClaim)).toEqual([])
  })
})
