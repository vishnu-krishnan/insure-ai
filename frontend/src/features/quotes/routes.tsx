import { Route, Routes } from 'react-router-dom'

import { QuoteCreatePage } from './QuoteCreatePage'
import { QuoteDetailPage } from './QuoteDetailPage'
import { QuoteListPage } from './QuoteListPage'

export function Component() {
  return (
    <Routes>
      <Route index element={<QuoteListPage />} />
      <Route path="new" element={<QuoteCreatePage />} />
      <Route path=":quoteId" element={<QuoteDetailPage />} />
    </Routes>
  )
}
