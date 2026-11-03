import {
  Badge,
  Loader,
  Box,
  Button,
  Card,
  Group,
  NativeSelect,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  ThemeIcon,
  UnstyledButton,
} from '@mantine/core'
import { IconArrowLeft, IconCheck, IconFilePlus, IconUser } from '@tabler/icons-react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'

import { createMyQuote, getMyProfile } from '../../api/me'
import { createQuote } from '../../api/quotes'
import {
  COVERAGE_TYPES,
  PAYMENT_FREQUENCIES,
  customerQuoteRequestSchema,
  emptyQuoteForm,
  type CustomerQuoteRequest,
} from '../../api/schemas'
import { COVER_GUIDES } from '../insurance/catalogue'
import { estimatePremium } from '../insurance/estimate'
import { MeHeader } from '../me/MeHeader'
import { completeness, isProfileComplete } from '../me/profileStatus'
import { formatMoney } from './format'
import { COVER_PHOTOS } from '../insurance/photos'
import { CoverPhoto } from '../shared/CoverPhoto'
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

// The shared schema wants a customer ID; for a customer's own quote this value is never sent.
const SELF_CUSTOMER_ID = '00000000'

const SELF_ERRORS = {
  PROFILE_REQUIRED: 'Complete your profile first, then request your quote.',
}

function ageOn(dateOfBirth: string): number {
  const born = new Date(dateOfBirth)
  const now = new Date()
  let age = now.getFullYear() - born.getFullYear()
  if (now.getMonth() < born.getMonth() || (now.getMonth() === born.getMonth() && now.getDate() < born.getDate())) {
    age -= 1
  }
  return age
}

function StepTitle({ n, children }: { n: number; children: string }) {
  return (
    <Group gap="sm" mt="xs">
      <ThemeIcon size={26} radius="xl" variant="filled" color="brand">
        <Text size="xs" fw={700} c="white">
          {n}
        </Text>
      </ThemeIcon>
      <Text fw={700} fz="lg">
        {children}
      </Text>
    </Group>
  )
}

// With mine set, the quote is for the signed-in customer: no customer is chosen and the server takes it from the token.
export function QuoteCreatePage({ type: fixedType, mine = false }: { type?: string; mine?: boolean } = {}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [params] = useSearchParams()
  const requestedType = COVER_GUIDES.find((item) => item.code === params.get('type'))?.code
  const { form, onSubmit, formError, isSubmitting } = useApiForm<CustomerQuoteRequest>(
    customerQuoteRequestSchema,
    fixedType
      ? {
          ...emptyQuoteForm,
          ...(mine ? { customerId: SELF_CUSTOMER_ID } : {}),
          coverageType: fixedType as CustomerQuoteRequest['coverageType'],
          details: emptyDetails(fixedType),
        }
      : mine
        ? {
            ...emptyQuoteForm,
            customerId: SELF_CUSTOMER_ID,
            ...(requestedType ? { coverageType: requestedType } : {}),
          }
        : emptyQuoteForm,
    async (values) => {
      if (mine) {
        const created = await createMyQuote(values)
        await queryClient.invalidateQueries({ queryKey: ['my-quotes'] })
        void navigate(`/me/quotes/${created.quoteId}`)
        return
      }
      const created = await createQuote(values)
      await queryClient.invalidateQueries({ queryKey: ['quotes'] })
      void navigate(`/agent/quotes/${created.quoteId}`)
    },
    mine ? { ...KNOWN_ERRORS, ...SELF_ERRORS } : KNOWN_ERRORS,
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
  const profile = useQuery({ queryKey: ['my-profile'], queryFn: getMyProfile, enabled: mine })
  const coverAmount = Number(watch('coverageAmount'))
  const estimate = mine && profile.data ? estimatePremium(type, coverAmount, ageOn(profile.data.dateOfBirth)) : null

  if (mine && profile.isPending) {
    return <Loader aria-label="Loading your profile" />
  }
  if (mine && !isProfileComplete(profile.data)) {
    const percent = profile.data ? completeness(profile.data) : 0
    return (
      <Stack gap="xl">
        <MeHeader eyebrow="My quotes" title="Get a quote" />
        <Card p={{ base: 'lg', sm: 40 }} radius="xl">
          <Stack align="center" gap="md" ta="center" maw={520} mx="auto">
            <ThemeIcon size={64} radius="xl" variant="light" color="brand">
              <IconUser size={34} stroke={1.5} />
            </ThemeIcon>
            <Text fw={800} fz={24}>
              Complete your profile first
            </Text>
            <Text c="dimmed">
              We price your cover from your profile, so every section needs to be filled in before you can request a
              quote. Your profile is {percent}% complete.
            </Text>
            <Button component={Link} to="/me/profile" size="md">
              {profile.data ? 'Finish my profile' : 'Complete my profile'}
            </Button>
          </Stack>
        </Card>
      </Stack>
    )
  }

  return (
    <form onSubmit={onSubmit} noValidate>
      <Stack>
        {mine ? (
          <Stack gap="xs">
            <Button
              component={Link}
              to="/me/quotes"
              variant="subtle"
              size="compact-md"
              leftSection={<IconArrowLeft size={16} />}
              w="fit-content"
            >
              My quotes
            </Button>
            <MeHeader
              eyebrow="My quotes"
              title="Get a quote"
              description="Choose your cover, tell us how much you need and see your premium straight away."
            />
          </Stack>
        ) : (
          <PageHeader
            icon={IconFilePlus}
            eyebrow={mine ? 'My quotes' : 'Quote service'}
            title={fixedType ? `New ${coverageStyle(fixedType).label.toLowerCase()} quote` : 'New quote'}
            description={
              mine
                ? 'Choose your cover and the premium is worked out when you save.'
                : 'Find the customer, enter the cover details and the premium is worked out when you save.'
            }
          />
        )}
        <SimpleGrid cols={{ base: 1, md: 3 }} spacing="lg" style={{ alignItems: 'start' }}>
          <Card p={{ base: 'md', sm: 'xl' }} style={{ gridColumn: 'span 2' }}>
            <Stack>
              <FormErrors error={formError} />
              {!mine && (
                <CustomerPicker
                  initialId={params.get('customerId') ?? ''}
                  error={errors.customerId?.message}
                  onChange={(customer) => setValue('customerId', customer?.customerId ?? '', { shouldValidate: false })}
                />
              )}
              {mine && !fixedType && (
                <>
                  <StepTitle n={1}>Choose your cover</StepTitle>
                  <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="sm">
                    {COVER_GUIDES.map((guide) => {
                      const selected = guide.code === type
                      const Glyph = coverageStyle(guide.code).icon
                      return (
                        <UnstyledButton
                          key={guide.code}
                          type="button"
                          aria-pressed={selected}
                          aria-label={guide.name}
                          onClick={() => setValue('coverageType', guide.code, { shouldValidate: false })}
                          p="md"
                          style={{
                            borderRadius: 'var(--mantine-radius-lg)',
                            border: `2px solid ${selected ? 'var(--mantine-color-brand-6)' : 'var(--mantine-color-default-border)'}`,
                            background: selected
                              ? 'light-dark(var(--mantine-color-brand-0), var(--mantine-color-dark-6))'
                              : 'transparent',
                            position: 'relative',
                            textAlign: 'center',
                          }}
                        >
                          {selected && (
                            <ThemeIcon size={20} radius="xl" color="brand" pos="absolute" top={8} right={8}>
                              <IconCheck size={13} />
                            </ThemeIcon>
                          )}
                          <ThemeIcon
                            size={44}
                            radius="md"
                            variant={selected ? 'filled' : 'light'}
                            color="brand"
                            mx="auto"
                          >
                            <Glyph size={24} stroke={1.6} />
                          </ThemeIcon>
                          <Text fw={650} mt="xs" size="sm">
                            {coverageStyle(guide.code).label}
                          </Text>
                        </UnstyledButton>
                      )
                    })}
                  </SimpleGrid>
                  <StepTitle n={2}>How much cover?</StepTitle>
                </>
              )}
              {!mine && !fixedType && (
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
              {mine ? (
                <StepTitle n={3}>Policy terms</StepTitle>
              ) : (
                <Text fw={650} mt="xs">
                  Policy terms
                </Text>
              )}
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
                <Button type="submit" size={mine ? 'md' : 'sm'} loading={isSubmitting}>
                  {mine ? 'Get my quote' : 'Create quote'}
                </Button>
              </Group>
            </Stack>
          </Card>
          <Card p={{ base: 'md', sm: 'xl' }} className="lift-soft" style={{ position: 'sticky', top: 16 }}>
            {COVER_PHOTOS[type] && (
              <Card.Section mb="md">
                <CoverPhoto
                  src={COVER_PHOTOS[type].card}
                  alt={COVER_PHOTOS[type].alt}
                  height={140}
                  position={COVER_PHOTOS[type].position}
                />
              </Card.Section>
            )}
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
              {estimate && (
                <Box
                  p="md"
                  style={{
                    borderRadius: 'var(--mantine-radius-md)',
                    background: 'var(--insure-navy-gradient)',
                    color: 'white',
                  }}
                >
                  <Badge variant="white" size="xs" mb={6}>
                    Estimate
                  </Badge>
                  <Text fw={800} fz={26} lh={1.1} c="white">
                    {formatMoney(estimate.annual)}
                    <Text span size="sm" fw={500} c="white" opacity={0.8}>
                      {' '}
                      / year
                    </Text>
                  </Text>
                  <Text size="xs" c="white" opacity={0.8}>
                    About {formatMoney(estimate.monthly)} a month before tax. Your saved quote is the final figure.
                  </Text>
                </Box>
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
