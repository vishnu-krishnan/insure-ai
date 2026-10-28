import { MantineProvider } from '@mantine/core'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { AuthProvider } from '../auth/AuthProvider'
import type { AuthUser } from '../auth/types'
import { server } from '../test/server'
import { stubClient } from '../test/stubClient'
import { createAppRoutes } from './routes'

function Boom(): never {
  throw new Error('render failure')
}

function renderAt(path: string, user: AuthUser | null) {
  const client = stubClient(user)
  const router = createMemoryRouter(createAppRoutes([{ path: '/boom', Component: Boom }]), {
    initialEntries: [path],
  })
  render(
    <MantineProvider>
      <QueryClientProvider client={new QueryClient()}>
        <AuthProvider client={client}>
          <RouterProvider router={router} />
        </AuthProvider>
      </QueryClientProvider>
    </MantineProvider>,
  )
  return client
}

const agent: AuthUser = { name: 'Ann Agent', roles: ['agent'] }
const customer: AuthUser = { name: 'Cy Customer', roles: ['customer'] }

describe('app routes', () => {
  it('starts sign-in when an anonymous visitor opens an agent page', async () => {
    const client = renderAt('/agent', null)

    await waitFor(() => expect(client.signIn).toHaveBeenCalledTimes(1))
  })

  function totals(customers: number, quotes: number) {
    const page = (totalElements: number) =>
      HttpResponse.json({ content: [], page: 0, size: 1, totalElements, totalPages: totalElements })
    server.use(
      http.get('/api/v1/customers', () => page(customers)),
      http.get('/api/v1/quotes', () => page(quotes)),
    )
  }

  it('shows each service with its live total and its actions', async () => {
    totals(12, 7)
    renderAt('/agent', agent)

    expect(await screen.findByText('12 customers registered')).toBeInTheDocument()
    expect(await screen.findByText('7 quotes issued')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Customer service' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Quote service' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /New customer/ })).toHaveAttribute('href', '/agent/customers/new')
    expect(screen.getByRole('link', { name: /New quote/ })).toHaveAttribute('href', '/agent/quotes/new')
    expect(screen.getAllByRole('link', { name: /Calculator/ }).length).toBeGreaterThan(0)
  })

  it('still shows the actions when a total cannot be loaded', async () => {
    server.use(
      http.get('/api/v1/customers', () => HttpResponse.json({ status: 500, code: 'INTERNAL_ERROR' }, { status: 500 })),
      http.get('/api/v1/quotes', () => HttpResponse.json({ status: 500, code: 'INTERNAL_ERROR' }, { status: 500 })),
    )
    renderAt('/agent', agent)

    expect(await screen.findByRole('link', { name: /New customer/ })).toBeInTheDocument()
    expect(screen.queryByText(/customers registered/)).not.toBeInTheDocument()
  })

  it('shows the dashboard to an agent', async () => {
    totals(0, 0)
    renderAt('/agent', agent)

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByText('Ann Agent')).toBeInTheDocument()
  })

  it('loads a feature screen lazily inside the shell', async () => {
    server.use(
      http.get('/api/v1/customers', () =>
        HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
      ),
    )
    renderAt('/agent/customers', agent)

    expect(await screen.findByRole('heading', { name: 'Customers' })).toBeInTheDocument()
  })

  it('shows the calculator to an anonymous visitor without starting sign-in', async () => {
    const client = renderAt('/calculator', null)

    expect(await screen.findByRole('heading', { name: 'Calculator' })).toBeInTheDocument()
    expect(client.signIn).not.toHaveBeenCalled()
  })

  it('opens a separate portal for each cover type and asks only the questions for that type', async () => {
    server.use(
      http.get('/api/v1/quotes', ({ request }) => {
        expect(new URL(request.url).searchParams.get('coverageType')).toBe('HEALTH')
        return HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
      }),
    )
    renderAt('/agent/health', agent)

    expect(await screen.findByRole('heading', { name: 'Health insurance' })).toBeInTheDocument()
    expect(await screen.findByText('No health quotes yet')).toBeInTheDocument()
  })

  it('shows vehicle questions on the auto quote form and none for other types', async () => {
    renderAt('/agent/auto/new', agent)

    expect(await screen.findByLabelText('Vehicle make')).toBeInTheDocument()
    expect(screen.queryByLabelText('Plan tier')).not.toBeInTheDocument()
  })

  it('does not list customers until the agent starts searching', async () => {
    renderAt('/agent/home/new', agent)

    expect(await screen.findByText(/Start typing to find a customer/)).toBeInTheDocument()
    expect(screen.queryByText('CUS-000001', { exact: false })).not.toBeInTheDocument()
  })

  it('offers the calculator inside the agent workspace', async () => {
    renderAt('/agent/calculator', agent)

    expect(await screen.findByRole('heading', { name: 'Calculator' })).toBeInTheDocument()
    expect(screen.getByText('Ann Agent')).toBeInTheDocument()
  })

  it('shows access denied to a customer on an agent page', async () => {
    renderAt('/agent', customer)

    expect(await screen.findByText('Access denied')).toBeInTheDocument()
  })

  it('sends a customer from the home page to the customer area notice', async () => {
    renderAt('/', customer)

    expect(await screen.findByText(/customer area is not available yet/i)).toBeInTheDocument()
  })

  it('sends an agent from the home page to the dashboard', async () => {
    renderAt('/', agent)

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
  })

  describe('landing page', () => {
    it('is shown to a visitor who is not signed in, without starting sign-in', async () => {
      const client = renderAt('/', null)

      expect(
        await screen.findByRole('heading', { level: 1, name: /Insurance that stays clear and simple/ }),
      ).toBeInTheDocument()
      expect(client.signIn).not.toHaveBeenCalled()
    })

    it('starts sign-in from the Sign in button', async () => {
      const client = renderAt('/', null)
      const user = userEvent.setup()

      await user.click((await screen.findAllByRole('button', { name: 'Sign in' }))[0]!)

      await waitFor(() => expect(client.signIn).toHaveBeenCalledTimes(1))
    })

    it('presents the four cover types', async () => {
      renderAt('/', null)

      for (const name of ['Auto insurance', 'Home insurance', 'Life insurance', 'Health insurance']) {
        expect(await screen.findByRole('heading', { name })).toBeInTheDocument()
      }
    })

    it('links to the calculator', async () => {
      renderAt('/', null)

      const links = await screen.findAllByRole('link', { name: /calculator/i })
      expect(links[0]).toHaveAttribute('href', '/calculator')
    })

    it('opens a frequently asked question to show its answer', async () => {
      renderAt('/', null)
      const user = userEvent.setup()

      await user.click(await screen.findByRole('button', { name: /How do I get a quote/ }))

      await waitFor(() => expect(screen.getByText(/Sign in as an agent/)).toBeVisible())
    })
  })

  describe('insurance plan pages', () => {
    it('lets a visitor compare the cover types without signing in', async () => {
      const client = renderAt('/insurance', null)

      expect(await screen.findByRole('heading', { level: 1, name: /Compare insurance plans/ })).toBeInTheDocument()
      for (const name of ['Auto insurance', 'Home insurance', 'Life insurance', 'Health insurance']) {
        expect(screen.getAllByRole('link', { name: new RegExp(`^${name}`) }).length).toBeGreaterThan(0)
      }
      expect(client.signIn).not.toHaveBeenCalled()
    })

    it('shows what a cover type includes and leaves out', async () => {
      renderAt('/insurance/health', null)

      expect(await screen.findByRole('heading', { level: 1, name: 'Health insurance' })).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: "What's covered" })).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: "What's not covered" })).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: 'Who it suits' })).toBeInTheDocument()
    })

    it('estimates the premium from the cover amount and age', async () => {
      renderAt('/insurance/home', null)

      expect(await screen.findByText('12,500.00')).toBeInTheDocument()
      expect(screen.getByText('1,041.67')).toBeInTheDocument()
    })

    it('says so for a cover type that does not exist', async () => {
      renderAt('/insurance/pet', null)

      expect(await screen.findByText('We do not offer that cover type')).toBeInTheDocument()
    })

    it('is reachable from the landing page cover cards', async () => {
      renderAt('/', null)

      const link = await screen.findByRole('link', { name: /Health insurance/ })
      expect(link).toHaveAttribute('href', '/insurance/health')
    })
  })

  it('shows a not-found page for an unknown path', async () => {
    renderAt('/nowhere', agent)

    expect(await screen.findByText('Page not found')).toBeInTheDocument()
  })

  it('shows a recoverable error page when a screen crashes', async () => {
    renderAt('/boom', agent)

    expect(await screen.findByText('Something went wrong')).toBeInTheDocument()
  })
})
