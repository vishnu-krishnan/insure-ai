import '@fontsource-variable/inter'
import '@mantine/core/styles.css'
import './global.css'

import { MantineProvider } from '@mantine/core'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { RouterProvider, createBrowserRouter } from 'react-router-dom'

import { ApiError } from '../api/problem'
import { AuthProvider } from '../auth/AuthProvider'
import { createAppRoutes } from './routes'
import { theme } from './theme'

function AppRouter() {
  const [router] = useState(() => createBrowserRouter(createAppRoutes()))
  return <RouterProvider router={router} />
}

export function App() {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: (count, error) =>
              !(error instanceof ApiError && error.status >= 400 && error.status < 500) && count < 2,
          },
        },
      }),
  )

  return (
    <MantineProvider theme={theme} defaultColorScheme="auto">
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <AppRouter />
        </AuthProvider>
      </QueryClientProvider>
    </MantineProvider>
  )
}
