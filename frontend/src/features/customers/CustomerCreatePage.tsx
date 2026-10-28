import { Button, Card, Group, NativeSelect, SimpleGrid, Stack, Text, TextInput } from '@mantine/core'
import { IconUserPlus } from '@tabler/icons-react'
import { useNavigate } from 'react-router-dom'

import { createCustomer } from '../../api/customers'
import {
  GENDERS,
  MARITAL_STATUSES,
  customerRequestSchema,
  emptyCustomerForm,
  toCustomerPayload,
  type CustomerRequest,
} from '../../api/schemas'
import { CountrySelect } from '../shared/CountrySelect'
import { PhoneField } from '../shared/PhoneField'
import { PageHeader } from '../shared/PageHeader'
import { FormErrors } from '../shared/FormErrors'
import { useApiForm } from '../shared/useApiForm'
import { labelFor } from './labels'

function Section({ title, hint, children }: { title: string; hint?: string; children: React.ReactNode }) {
  return (
    <Card p={{ base: 'md', sm: 'xl' }}>
      <Stack>
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
        {children}
      </Stack>
    </Card>
  )
}

export function CustomerCreatePage() {
  const navigate = useNavigate()
  const { form, onSubmit, formError, isSubmitting } = useApiForm<CustomerRequest>(
    customerRequestSchema,
    emptyCustomerForm,
    async (values) => {
      const created = await createCustomer(toCustomerPayload(values))
      void navigate(`/agent/customers/${created.customerId}`)
    },
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
        <PageHeader
          icon={IconUserPlus}
          eyebrow="Customer service"
          title="New customer"
          description="Add the policyholder's details once. You can quote any type of cover for them afterwards."
        />
        <FormErrors error={formError} />
        <SimpleGrid cols={{ base: 1, lg: 2 }} spacing="lg" style={{ alignItems: 'start' }}>
          <Stack>
            <Section title="Personal details">
              <TextInput
                label="Full name"
                autoComplete="name"
                error={errors.fullName?.message}
                {...register('fullName')}
              />
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput
                  label="Date of birth"
                  type="date"
                  error={errors.dateOfBirth?.message}
                  {...register('dateOfBirth')}
                />
                <NativeSelect
                  label="Gender"
                  data={[
                    { value: '', label: 'Not stated' },
                    ...GENDERS.map((value) => ({ value, label: labelFor(value) })),
                  ]}
                  error={errors.gender?.message}
                  {...register('gender')}
                />
                <NativeSelect
                  label="Marital status"
                  data={[
                    { value: '', label: 'Not stated' },
                    ...MARITAL_STATUSES.map((value) => ({ value, label: labelFor(value) })),
                  ]}
                  error={errors.maritalStatus?.message}
                  {...register('maritalStatus')}
                />
                <TextInput label="Occupation" error={errors.occupation?.message} {...register('occupation')} />
              </SimpleGrid>
              <TextInput
                label="Annual income"
                inputMode="decimal"
                placeholder="1200000"
                error={errors.annualIncome?.message}
                {...register('annualIncome')}
              />
            </Section>
            <Section title="Contact">
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput
                  label="Email"
                  type="email"
                  autoComplete="email"
                  error={errors.email?.message}
                  {...register('email')}
                />
                <PhoneField
                  label="Phone"
                  value={getValues('phone')}
                  error={errors.phone?.message}
                  onChange={(value) => setValue('phone', value, { shouldValidate: isSubmitted })}
                />
              </SimpleGrid>
            </Section>
          </Stack>
          <Stack>
            <Section
              title="Address"
              hint="Optional, but a quote is easier to issue with one on file. If you start an address, complete every line except line 2."
            >
              <TextInput
                label="Address line 1"
                autoComplete="address-line1"
                error={errors.address?.line1?.message}
                {...register('address.line1')}
              />
              <TextInput
                label="Address line 2"
                autoComplete="address-line2"
                error={errors.address?.line2?.message}
                {...register('address.line2')}
              />
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput label="City" error={errors.address?.city?.message} {...register('address.city')} />
                <TextInput label="State" error={errors.address?.state?.message} {...register('address.state')} />
                <TextInput
                  label="Postal code"
                  autoComplete="postal-code"
                  error={errors.address?.postalCode?.message}
                  {...register('address.postalCode')}
                />
                <CountrySelect
                  label="Country"
                  value={watch('address.country')}
                  error={errors.address?.country?.message}
                  onChange={(code) => setValue('address.country', code, { shouldValidate: isSubmitted })}
                />
              </SimpleGrid>
            </Section>
            <Section title="Nominee" hint="Optional. The person who receives the benefit if a claim is paid out.">
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput label="Nominee name" error={errors.nominee?.name?.message} {...register('nominee.name')} />
                <TextInput
                  label="Relationship"
                  placeholder="Spouse"
                  error={errors.nominee?.relationship?.message}
                  {...register('nominee.relationship')}
                />
              </SimpleGrid>
            </Section>
          </Stack>
        </SimpleGrid>
        <Group justify="flex-end">
          <Button type="submit" loading={isSubmitting}>
            Create customer
          </Button>
        </Group>
      </Stack>
    </form>
  )
}
