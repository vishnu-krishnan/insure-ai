import { Route, Routes } from 'react-router-dom'

import { QuoteCreatePage } from '../quotes/QuoteCreatePage'
import { QuoteDetailPage } from '../quotes/QuoteDetailPage'
import { QuoteListPage } from '../quotes/QuoteListPage'

export function PortalRoutes({ type }: { type: string }) {
  return (
    <Routes>
      <Route index element={<QuoteListPage type={type} />} />
      <Route path="new" element={<QuoteCreatePage type={type} />} />
      <Route path=":quoteId" element={<QuoteDetailPage />} />
    </Routes>
  )
}
