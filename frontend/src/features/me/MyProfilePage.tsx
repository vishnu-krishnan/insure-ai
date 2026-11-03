import { Alert, Loader } from '@mantine/core'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'

import { ACCOUNT_EMAIL_PLACEHOLDER, getMyProfile, saveMyProfile } from '../../api/me'
import { emptyCustomerForm, type Customer, type CustomerRequest } from '../../api/schemas'
import { CustomerCreatePage } from '../customers/CustomerCreatePage'
import { useAuth } from '../../auth/useAuth'
import { describeError } from '../shared/errors'
import { MeHeader } from './MeHeader'

function toForm(customer: Customer): CustomerRequest {
  return {
    fullName: customer.fullName,
    email: customer.email,
    phone: customer.phone,
    dateOfBirth: customer.dateOfBirth,
    gender: (customer.gender ?? '') as CustomerRequest['gender'],
    maritalStatus: (customer.maritalStatus ?? '') as CustomerRequest['maritalStatus'],
    occupation: customer.occupation ?? '',
    idType: (customer.idType ?? '') as CustomerRequest['idType'],
    idNumber: customer.idNumber ?? '',
    annualIncome: customer.annualIncome == null ? '' : String(customer.annualIncome),
    address: {
      line1: customer.address?.line1 ?? '',
      line2: customer.address?.line2 ?? '',
      city: customer.address?.city ?? '',
      state: customer.address?.state ?? '',
      postalCode: customer.address?.postalCode ?? '',
      country: customer.address?.country ?? '',
    },
    nominee: { name: customer.nominee?.name ?? '', relationship: customer.nominee?.relationship ?? '' },
  }
}

export function MyProfilePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const profile = useQuery({ queryKey: ['my-profile'], queryFn: getMyProfile })

  if (profile.isPending) {
    return <Loader aria-label="Loading your profile" />
  }
  if (profile.isError) {
    return (
      <Alert color="red" title={describeError(profile.error).message}>
        Please refresh the page to try again.
      </Alert>
    )
  }

  const existing = profile.data
  return (
    <CustomerCreatePage
      key={existing?.customerId ?? 'new'}
      emailLocked
      strict
      nameLocked={existing == null && Boolean(user?.fullName)}
      initial={
        existing
          ? toForm(existing)
          : { ...emptyCustomerForm, email: ACCOUNT_EMAIL_PLACEHOLDER, fullName: user?.fullName ?? '' }
      }
      header={
        <MeHeader
          eyebrow="My account"
          title={existing ? 'My profile' : 'Complete your profile'}
          description={
            existing
              ? 'Keep your details up to date. Your sign-in email cannot be changed here.'
              : 'Save as you go and finish later. Fields marked * must be filled in before you can request a quote.'
          }
        />
      }
      submitLabel={existing ? 'Save changes' : 'Save profile'}
      onSave={async (payload) => {
        await saveMyProfile(payload, existing != null)
        await queryClient.invalidateQueries({ queryKey: ['my-profile'] })
        void navigate('/me')
      }}
    />
  )
}
