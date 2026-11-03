import { Button, Card, Group, NativeSelect, SimpleGrid, Stack, Text, TextInput, ThemeIcon } from '@mantine/core'
import { IconAddressBook, IconHeartHandshake, IconMapPin, IconUser, IconUserPlus, type Icon } from '@tabler/icons-react'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

import { createCustomer } from '../../api/customers'
import { ACCOUNT_EMAIL_PLACEHOLDER } from '../../api/me'
import {
  GENDERS,
  ID_TYPES,
  ID_TYPE_INFO,
  MARITAL_STATUSES,
  customerRequestSchema,
  myProfileSchema,
  emptyCustomerForm,
  toCustomerPayload,
  type CustomerRequest,
} from '../../api/schemas'
import { CountrySelect } from '../shared/CountrySelect'
import { DateOfBirthField } from './DateOfBirthField'
import { OccupationField } from './OccupationField'
import { StateSelect } from '../shared/StateSelect'
import { PhoneField } from '../shared/PhoneField'
import { PageHeader } from '../shared/PageHeader'
import { FormErrors } from '../shared/FormErrors'
import { useApiForm } from '../shared/useApiForm'
import { labelFor } from './labels'

const RELATIONSHIPS = ['Spouse', 'Son', 'Daughter', 'Father', 'Mother', 'Brother', 'Sister', 'Grandparent', 'Other']

function Section({
  title,
  hint,
  icon: Glyph,
  children,
}: {
  title: string
  hint?: string
  icon?: Icon
  children: React.ReactNode
}) {
  return (
    <Card p={{ base: 'md', sm: 'xl' }}>
      <Stack>
        <Group gap="sm" wrap="nowrap" align="flex-start">
          {Glyph && (
            <ThemeIcon size={36} radius="md" variant="light" color="brand">
              <Glyph size={20} stroke={1.6} />
            </ThemeIcon>
          )}
          <Stack gap={2}>
            <Text fw={650} fz="lg">
              {title}
            </Text>
            {hint && (
              <Text size="sm" c="dimmed">
                {hint}
              </Text>
            )}
          </Stack>
        </Group>
        {children}
      </Stack>
    </Card>
  )
}

interface CustomerFormProps {
  /** Overrides the default "create then open the agent customer page" behaviour. */
  onSave?: (payload: ReturnType<typeof toCustomerPayload>) => Promise<void>
  initial?: CustomerRequest
  /** The email comes from the sign-in account and cannot be edited. */
  emailLocked?: boolean
  /** The full name comes from the sign-up details and is shown read-only. */
  nameLocked?: boolean
  title?: string
  eyebrow?: string
  description?: string
  submitLabel?: string
  /** Replaces the standard page header, and adds icons to the section cards. */
  header?: ReactNode
  /** A customer's own profile: every field is required and occupation is asked as a short question. */
  strict?: boolean
}

export function CustomerCreatePage({
  onSave,
  initial,
  emailLocked = false,
  nameLocked = false,
  title = 'New customer',
  eyebrow = 'Customer service',
  description = "Add the policyholder's details once. You can quote any type of cover for them afterwards.",
  submitLabel = 'Create customer',
  header,
  strict = false,
}: CustomerFormProps = {}) {
  const navigate = useNavigate()
  const { form, onSubmit, formError, isSubmitting } = useApiForm<CustomerRequest>(
    strict ? myProfileSchema : customerRequestSchema,
    initial ?? emptyCustomerForm,
    async (values) => {
      if (onSave) {
        await onSave(toCustomerPayload(values))
        return
      }
      const created = await createCustomer(toCustomerPayload(values))
      void navigate(`/agent/customers/${created.customerId}`)
    },
    {},
    'onTouched',
  )
  const {
    register,
    watch,
    getValues,
    setValue,
    formState: { errors, isSubmitted },
  } = form

  return (
    <form onSubmit={onSubmit} noValidate>
      <Stack>
        {header ?? <PageHeader icon={IconUserPlus} eyebrow={eyebrow} title={title} description={description} />}
        <FormErrors error={formError} />
        <SimpleGrid cols={{ base: 1, lg: header ? 1 : 2 }} spacing="lg" style={{ alignItems: 'start' }}>
          <Stack>
            <Section title="Personal details" icon={header ? IconUser : undefined}>
              <TextInput
                label="Full name"
                withAsterisk={strict}
                maxLength={80}
                readOnly={nameLocked}
                description={nameLocked ? 'Taken from your account' : undefined}
                autoComplete="name"
                error={errors.fullName?.message}
                {...register('fullName')}
              />
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                strict ? (
                <DateOfBirthField
                  value={watch('dateOfBirth')}
                  error={errors.dateOfBirth?.message}
                  onChange={(value) => setValue('dateOfBirth', value, { shouldValidate: true })}
                />
                ) : (
                <TextInput
                  label="Date of birth"
                  type="date"
                  error={errors.dateOfBirth?.message}
                  {...register('dateOfBirth')}
                />
                )
                <NativeSelect
                  label="Gender"
                  withAsterisk={strict}
                  data={[
                    { value: '', label: strict ? 'Choose' : 'Not stated' },
                    ...GENDERS.map((value) => ({ value, label: labelFor(value) })),
                  ]}
                  error={errors.gender?.message}
                  {...register('gender')}
                />
                <NativeSelect
                  label="Marital status"
                  withAsterisk={strict}
                  data={[
                    { value: '', label: strict ? 'Choose' : 'Not stated' },
                    ...MARITAL_STATUSES.map((value) => ({ value, label: labelFor(value) })),
                  ]}
                  error={errors.maritalStatus?.message}
                  {...register('maritalStatus')}
                />
                {!strict && (
                  <TextInput
                    label="Occupation"
                    maxLength={80}
                    error={errors.occupation?.message}
                    {...register('occupation')}
                  />
                )}
              </SimpleGrid>
              {strict && (
                <OccupationField
                  value={watch('occupation')}
                  error={errors.occupation?.message}
                  onChange={(value) => setValue('occupation', value, { shouldValidate: true })}
                />
              )}
              <TextInput
                label="Annual income"
                withAsterisk={strict}
                maxLength={15}
                inputMode="decimal"
                placeholder="1200000"
                error={errors.annualIncome?.message}
                {...register('annualIncome')}
              />
            </Section>
            <Section title={strict ? 'Contact and ID' : 'Contact'} icon={header ? IconAddressBook : undefined}>
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                {!(emailLocked && initial?.email === ACCOUNT_EMAIL_PLACEHOLDER) && (
                  <TextInput
                    label="Email"
                    type="email"
                    autoComplete="email"
                    readOnly={emailLocked}
                    error={errors.email?.message}
                    {...register('email')}
                  />
                )}
                <PhoneField
                  label="Phone"
                  value={getValues('phone')}
                  error={errors.phone?.message}
                  onChange={(value) => setValue('phone', value, { shouldValidate: isSubmitted })}
                />
              </SimpleGrid>
              {strict && (
                <SimpleGrid cols={{ base: 1, sm: 2 }}>
                  <NativeSelect
                    label="ID type"
                    withAsterisk
                    data={[
                      { value: '', label: 'Choose' },
                      ...ID_TYPES.map((value) => ({ value, label: ID_TYPE_INFO[value].label })),
                    ]}
                    error={errors.idType?.message}
                    {...register('idType')}
                  />
                  <TextInput
                    label="ID number"
                    withAsterisk
                    maxLength={30}
                    autoComplete="off"
                    placeholder={
                      watch('idType') ? ID_TYPE_INFO[watch('idType') as keyof typeof ID_TYPE_INFO].example : ''
                    }
                    description="As printed on your ID"
                    error={errors.idNumber?.message}
                    {...register('idNumber')}
                  />
                </SimpleGrid>
              )}
            </Section>
          </Stack>
          <Stack>
            <Section title="Address" icon={header ? IconMapPin : undefined}>
              <TextInput
                label="Address line 1"
                withAsterisk={strict}
                maxLength={120}
                autoComplete="address-line1"
                error={errors.address?.line1?.message}
                {...register('address.line1')}
              />
              <TextInput
                label="Address line 2"
                maxLength={120}
                autoComplete="address-line2"
                error={errors.address?.line2?.message}
                {...register('address.line2')}
              />
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <CountrySelect
                  label="Country"
                  withAsterisk={strict}
                  value={watch('address.country')}
                  error={errors.address?.country?.message}
                  onChange={(code) => {
                    setValue('address.country', code, { shouldValidate: isSubmitted })
                    setValue('address.state', '', { shouldValidate: isSubmitted })
                  }}
                />
                <StateSelect
                  label="State"
                  withAsterisk={strict}
                  country={watch('address.country')}
                  value={watch('address.state')}
                  error={errors.address?.state?.message}
                  onChange={(state) => setValue('address.state', state, { shouldValidate: isSubmitted })}
                />
                <TextInput
                  label="City"
                  withAsterisk={strict}
                  maxLength={80}
                  error={errors.address?.city?.message}
                  {...register('address.city')}
                />
                <TextInput
                  label="Postal code"
                  withAsterisk={strict}
                  maxLength={12}
                  autoComplete="postal-code"
                  error={errors.address?.postalCode?.message}
                  {...register('address.postalCode')}
                />
              </SimpleGrid>
            </Section>
            <Section
              title="Nominee"
              icon={header ? IconHeartHandshake : undefined}
              hint="Optional. Enter the nominee's name exactly as it appears on their ID card."
            >
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput
                  label="Nominee name"
                  withAsterisk={strict}
                  description="As shown on their ID card"
                  maxLength={80}
                  error={errors.nominee?.name?.message}
                  {...register('nominee.name')}
                />
                <NativeSelect
                  label="Relationship"
                  withAsterisk={strict}
                  data={[
                    { value: '', label: 'Choose' },
                    ...(watch('nominee.relationship') && !RELATIONSHIPS.includes(watch('nominee.relationship'))
                      ? [{ value: watch('nominee.relationship'), label: watch('nominee.relationship') }]
                      : []),
                    ...RELATIONSHIPS.map((value) => ({ value, label: value })),
                  ]}
                  error={errors.nominee?.relationship?.message}
                  {...register('nominee.relationship')}
                />
              </SimpleGrid>
            </Section>
          </Stack>
        </SimpleGrid>
        <Group justify="flex-end">
          <Button type="submit" size={header ? 'md' : 'sm'} loading={isSubmitting}>
            {submitLabel}
          </Button>
        </Group>
      </Stack>
    </form>
  )
}
