import { Route, Routes } from 'react-router-dom'

import { CustomerCreatePage } from './CustomerCreatePage'
import { CustomerDetail } from './CustomerDetail'
import { CustomerSearchPage } from './CustomerSearchPage'

export function Component() {
  return (
    <Routes>
      <Route index element={<CustomerSearchPage />} />
      <Route path="new" element={<CustomerCreatePage />} />
      <Route path=":customerId" element={<CustomerDetail />} />
    </Routes>
  )
}
