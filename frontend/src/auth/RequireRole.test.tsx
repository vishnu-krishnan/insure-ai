import { screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { renderWithMantine } from '../test/render'
import { stubClient } from '../test/stubClient'
import { AuthProvider } from './AuthProvider'
import { RequireRole } from './RequireRole'
import type { AuthClient } from './types'

function renderGate(client: AuthClient) {
  return renderWithMantine(
    <AuthProvider client={client}>
      <RequireRole role="agent">
        <p>agent only</p>
      </RequireRole>
    </AuthProvider>,
  )
}

describe('RequireRole', () => {
  it('shows the content to a user with the role', async () => {
    renderGate(stubClient({ name: 'Ann', roles: ['agent'] }))

    expect(await screen.findByText('agent only')).toBeInTheDocument()
  })

  it('shows access denied to a user without the role', async () => {
    renderGate(stubClient({ name: 'Cy', roles: ['customer'] }))

    expect(await screen.findByText('Access denied')).toBeInTheDocument()
    expect(screen.queryByText('agent only')).not.toBeInTheDocument()
  })

  it('starts sign-in for an anonymous visitor', async () => {
    const client = stubClient(null)

    renderGate(client)

    await waitFor(() => expect(client.signIn).toHaveBeenCalledTimes(1))
    expect(screen.queryByText('agent only')).not.toBeInTheDocument()
  })
})
