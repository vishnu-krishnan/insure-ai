import { Box, Button, Card, Group, NativeSelect, SimpleGrid, Stack, Text, TextInput, ThemeIcon } from '@mantine/core'
import { IconFilePlus } from '@tabler/icons-react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router-dom'

import { createQuote } from '../../api/quotes'
import {
  COVERAGE_TYPES,
  PAYMENT_FREQUENCIES,
  customerQuoteRequestSchema,
  emptyQuoteForm,
  type CustomerQuoteRequest,
} from '../../api/schemas'
import { COVER_GUIDES } from '../insurance/catalogue'
import { coverageStyle } from '../shared/coverage'
import { TYPE_FIELDS, emptyDetails, fieldChoices } from '../portal/typeFields'
import { FormErrors } from '../shared/FormErrors'
import { PageHeader } from '../shared/PageHeader'
import { useApiForm } from '../shared/useApiForm'
import { CustomerPicker } from './CustomerPicker'

const KNOWN_ERRORS = {
  CUSTOMER_REFERENCE_INVALID: 'That customer does not exist',
  CUSTOMER_NOT_FOUND: 'That customer does not exist',
  CUSTOMER_SERVICE_UNAVAILABLE: 'The customer service is not responding. Try again in a moment.',
  CUSTOMER_SERVICE_TIMEOUT: 'The customer service is not responding. Try again in a moment.',
}

export function QuoteCreatePage({ type: fixedType }: { type?: string } = {}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [params] = useSearchParams()
  const { form, onSubmit, formError, isSubmitting } = useApiForm<CustomerQuoteRequest>(
    customerQuoteRequestSchema,
    fixedType
      ? {
          ...emptyQuoteForm,
          coverageType: fixedType as CustomerQuoteRequest['coverageType'],
          details: emptyDetails(fixedType),
        }
      : emptyQuoteForm,
    async (values) => {
      const created = await createQuote(values)
      await queryClient.invalidateQueries({ queryKey: ['quotes'] })
      void navigate(`/agent/quotes/${created.quoteId}`)
    },
    KNOWN_ERRORS,
  )
  const {
    register,
    setValue,
    watch,
    formState: { errors },
  } = form
  const type = watch('coverageType')
  const guide = COVER_GUIDES.find((item) => item.code === type)
  const style = coverageStyle(type)
  const CoverIcon = style.icon

  return (
    <form onSubmit={onSubmit} noValidate>
      <Stack>
        <PageHeader
          icon={IconFilePlus}
          eyebrow="Quote service"
          title={fixedType ? `New ${coverageStyle(fixedType).label.toLowerCase()} quote` : 'New quote'}
          description="Find the customer, enter the cover details and the premium is worked out when you save."
        />
        <SimpleGrid cols={{ base: 1, md: 3 }} spacing="lg" style={{ alignItems: 'start' }}>
          <Card p={{ base: 'md', sm: 'xl' }} style={{ gridColumn: 'span 2' }}>
            <Stack>
              <FormErrors error={formError} />
              <CustomerPicker
                initialId={params.get('customerId') ?? ''}
                error={errors.customerId?.message}
                onChange={(customer) => setValue('customerId', customer?.customerId ?? '', { shouldValidate: false })}
              />
              {!fixedType && (
                <NativeSelect
                  label="Coverage type"
                  data={[...COVERAGE_TYPES]}
                  error={errors.coverageType?.message}
                  {...register('coverageType')}
                />
              )}
              <TextInput
                label="Coverage amount"
                inputMode="decimal"
                placeholder="250000.00"
                error={errors.coverageAmount?.message}
                {...register('coverageAmount')}
              />
              {fixedType && (
                <>
                  <Text fw={650} mt="xs">
                    {`${coverageStyle(fixedType).label} details`}
                  </Text>
                  <SimpleGrid cols={{ base: 1, sm: 2 }}>
                    {(TYPE_FIELDS[fixedType] ?? []).map((field) => {
                      const error = errors.details?.[field.key]?.message
                      const name = `details.${field.key}` as const
                      return field.kind === 'text' || field.kind === 'int' ? (
                        <TextInput
                          key={field.key}
                          label={field.label}
                          placeholder={field.placeholder}
                          inputMode={field.kind === 'int' ? 'numeric' : undefined}
                          error={error}
                          {...register(name)}
                        />
                      ) : (
                        <NativeSelect
                          key={field.key}
                          label={field.label}
                          data={[{ value: '', label: 'Choose' }, ...fieldChoices(field)]}
                          error={error}
                          {...register(name)}
                        />
                      )
                    })}
                  </SimpleGrid>
                </>
              )}
              <Text fw={650} mt="xs">
                Policy terms
              </Text>
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput
                  label="Policy term (years)"
                  inputMode="numeric"
                  placeholder="1"
                  description="From 1 to 40 years. Leave blank for 1 year."
                  error={errors.policyTermYears?.message}
                  {...register('policyTermYears')}
                />
                <NativeSelect
                  label="Payment frequency"
                  description="How the customer pays. Leave blank for yearly."
                  data={[
                    { value: '', label: 'Yearly (default)' },
                    ...PAYMENT_FREQUENCIES.map((value) => ({
                      value,
                      label: value.charAt(0) + value.slice(1).toLowerCase(),
                    })),
                  ]}
                  error={errors.paymentFrequency?.message}
                  {...register('paymentFrequency')}
                />
                <TextInput
                  label="Deductible"
                  inputMode="decimal"
                  placeholder="0"
                  description="What the customer pays first on a claim. Must be below the cover amount."
                  error={errors.deductible?.message}
                  {...register('deductible')}
                />
                <TextInput
                  label="Start date"
                  type="date"
                  description="When cover begins, from today up to one year ahead. Leave blank for today."
                  error={errors.startDate?.message}
                  {...register('startDate')}
                />
              </SimpleGrid>
              <Group justify="flex-end" mt="xs">
                <Button type="submit" loading={isSubmitting}>
                  Create quote
                </Button>
              </Group>
            </Stack>
          </Card>
          <Card p={{ base: 'md', sm: 'xl' }} className="lift-soft" style={{ position: 'sticky', top: 16 }}>
            <Stack gap="sm">
              <Group gap="sm" wrap="nowrap">
                <ThemeIcon size={40} radius="md" variant="light" color={style.color}>
                  <CoverIcon size={22} />
                </ThemeIcon>
                <Text fw={650} fz="lg">
                  {style.label} cover
                </Text>
              </Group>
              {guide && (
                <>
                  <Text size="sm" c="dimmed">
                    {guide.summary}
                  </Text>
                  <Box>
                    <Text size="xs" fw={650} tt="uppercase" c="dimmed" mb={4}>
                      Premium basis
                    </Text>
                    <Text size="sm">{guide.premiumBasis}</Text>
                  </Box>
                </>
              )}
              <Text size="xs" c="dimmed">
                Age 18 to 75 at the time of the quote. Tax is added to the premium you see after saving.
              </Text>
            </Stack>
          </Card>
        </SimpleGrid>
      </Stack>
    </form>
  )
}
