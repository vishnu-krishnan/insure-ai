import { Route, Routes } from 'react-router-dom'

import { QuoteCreatePage } from '../quotes/QuoteCreatePage'
import { QuoteDetailPage } from '../quotes/QuoteDetailPage'
import { MeDashboard } from './MeDashboard'
import { MeLayout } from './MeLayout'
import { MyProfilePage } from './MyProfilePage'
import { MyQuotesPage } from './MyQuotesPage'

export function Component() {
  return (
    <Routes>
      <Route element={<MeLayout />}>
        <Route index element={<MeDashboard />} />
        <Route path="profile" element={<MyProfilePage />} />
        <Route path="quotes" element={<MyQuotesPage />} />
        <Route path="quotes/new" element={<QuoteCreatePage mine />} />
        <Route path="quotes/:quoteId" element={<QuoteDetailPage mine />} />
      </Route>
    </Routes>
  )
}
