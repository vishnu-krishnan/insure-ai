import { Navigate, type RouteObject } from 'react-router-dom'

import { RequireRole } from '../auth/RequireRole'
import { CustomerAreaPlaceholder } from './pages/CustomerAreaPlaceholder'
import { Dashboard } from './pages/Dashboard'
import { ErrorFallback } from './pages/ErrorFallback'
import { Home } from './pages/Home'
import { NotFound } from './pages/NotFound'
import { PublicLayout } from './pages/PublicLayout'
import { Shell } from './Shell'
import { InsuranceDetailPage } from '../features/insurance/InsuranceDetailPage'
import { InsuranceIndexPage } from '../features/insurance/InsuranceIndexPage'
import { COVER_GUIDES } from '../features/insurance/catalogue'
import { PortalRoutes } from '../features/portal/routes'
import { CalculatorPage } from '../features/calculator/CalculatorPage'

export function createAppRoutes(extra: RouteObject[] = []): RouteObject[] {
  return [
    {
      errorElement: <ErrorFallback />,
      children: [
        {
          element: <PublicLayout wide />,
          children: [{ path: '/', element: <Home /> }],
        },
        { path: '/auth/callback', element: <Navigate to="/" replace /> },
        {
          element: <PublicLayout wide />,
          children: [
            { path: '/insurance', element: <InsuranceIndexPage /> },
            { path: '/insurance/:type', element: <InsuranceDetailPage /> },
          ],
        },
        {
          element: <PublicLayout />,
          children: [{ path: '/calculator', element: <CalculatorPage /> }],
        },
        {
          path: '/agent',
          element: (
            <RequireRole role="agent">
              <Shell />
            </RequireRole>
          ),
          children: [
            { index: true, element: <Dashboard /> },
            { path: 'calculator', element: <CalculatorPage /> },
            { path: 'customers/*', lazy: () => import('../features/customers/routes') },
            { path: 'quotes/*', lazy: () => import('../features/quotes/routes') },
            ...COVER_GUIDES.map((guide) => ({ path: `${guide.slug}/*`, element: <PortalRoutes type={guide.code} /> })),
          ],
        },
        {
          path: '/me/*',
          element: (
            <RequireRole role="customer">
              <CustomerAreaPlaceholder />
            </RequireRole>
          ),
        },
        ...extra,
        { path: '*', element: <NotFound /> },
      ],
    },
  ]
}
