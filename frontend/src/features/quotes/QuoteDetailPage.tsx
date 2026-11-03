import { Alert, Anchor, Badge, Box, Button, Card, Divider, Group, Loader, SimpleGrid, Stack, Text } from '@mantine/core'
import { IconArrowLeft, IconFileText } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'

import { getMyQuote } from '../../api/me'
import { getQuote } from '../../api/quotes'
import type { QuoteSummary } from '../../api/schemas'
import { COVER_PHOTOS } from '../insurance/photos'
import { CoverPhoto } from '../shared/CoverPhoto'
import { coverageStyle } from '../shared/coverage'
import { CoverageBadge } from '../shared/CoverageBadge'
import { DetailRow } from '../shared/DetailRow'
import { PageHeader } from '../shared/PageHeader'
import { RefLabel } from '../shared/RefLabel'
import { describeError } from '../shared/errors'
import { describeDetails } from '../portal/typeFields'
import { formatMoney } from './format'

const FREQUENCY_LABEL: Record<string, string> = {
  MONTHLY: 'Monthly instalment',
  QUARTERLY: 'Quarterly instalment',
  YEARLY: 'Yearly payment',
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card p={{ base: 'md', sm: 'xl' }}>
      <Stack gap="sm">
        <Text fw={650} fz="lg">
          {title}
        </Text>
        {children}
      </Stack>
    </Card>
  )
}

function Status({ status }: { status: string }) {
  const expired = status === 'EXPIRED'
  return (
    <Badge color={expired ? 'orange' : 'teal'} variant="light">
      {expired ? 'Expired' : 'Issued'}
    </Badge>
  )
}

function Premium({ quote }: { quote: QuoteSummary }) {
  const hasBreakdown = quote.totalAnnualPayable != null && quote.taxAmount != null
  return (
    <Section title="Premium">
      <DetailRow label="Annual premium" value={formatMoney(quote.annualPremium)} />
      <DetailRow label="Monthly premium" value={formatMoney(quote.monthlyPremium)} />
      {hasBreakdown && (
        <>
          <Divider />
          <DetailRow
            label={`Tax (${Math.round((quote.taxRate ?? 0) * 10000) / 100}%)`}
            value={formatMoney(quote.taxAmount!)}
          />
          <DetailRow label="Total annual payable" value={formatMoney(quote.totalAnnualPayable!)} />
          {quote.installmentAmount != null && (
            <DetailRow
              label={FREQUENCY_LABEL[quote.paymentFrequency ?? 'YEARLY'] ?? 'Instalment'}
              value={formatMoney(quote.installmentAmount)}
            />
          )}
        </>
      )}
    </Section>
  )
}

function MineHero({ quote, backTo }: { quote: QuoteSummary; backTo: string }) {
  const photo = COVER_PHOTOS[quote.coverageType]
  const style = coverageStyle(quote.coverageType)
  return (
    <Stack gap="md">
      <Button
        component={Link}
        to={backTo}
        variant="subtle"
        size="compact-md"
        leftSection={<IconArrowLeft size={16} />}
        w="fit-content"
      >
        All quotes
      </Button>
      <Card p={0} radius="xl" style={{ overflow: 'hidden', border: 'none', background: 'var(--insure-navy-gradient)' }}>
        <Box pos="relative">
          {photo && (
            <Box h={{ base: 120, sm: 150 }} style={{ overflow: 'hidden' }}>
              <img
                src={photo.banner}
                alt={photo.alt}
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'cover',
                  objectPosition: photo.position,
                  display: 'block',
                }}
              />
              <Box
                pos="absolute"
                inset={0}
                style={{ background: 'linear-gradient(180deg, rgba(4,12,38,0.1), rgba(4,12,38,0.85))' }}
              />
            </Box>
          )}
        </Box>
        <Group justify="space-between" align="flex-end" p={{ base: 'lg', sm: 'xl' }} gap="lg">
          <Stack gap={4}>
            <Group gap="sm">
              <Text c="white" opacity={0.8} size="sm" fw={650} tt="uppercase" style={{ letterSpacing: 1 }}>
                {style.label} cover
              </Text>
              {quote.status && <Status status={quote.status} />}
            </Group>
            <Text c="white" fw={800} fz={{ base: 26, sm: 32 }} lh={1.15}>
              Quote {quote.quoteNumber}
            </Text>
            <Text c="white" opacity={0.8}>
              Cover of {formatMoney(quote.coverageAmount)}
            </Text>
          </Stack>
          <Stack gap={0} align="flex-end">
            <Text c="white" fw={800} fz={{ base: 30, sm: 40 }} lh={1.1}>
              {formatMoney(quote.annualPremium)}
            </Text>
            <Text c="white" opacity={0.8} size="sm">
              per year · {formatMoney(quote.monthlyPremium)} a month
            </Text>
          </Stack>
        </Group>
      </Card>
    </Stack>
  )
}

export function QuoteDetailPage({ mine = false }: { mine?: boolean } = {}) {
  const { quoteId = '' } = useParams()
  const backTo = mine ? '/me/quotes' : '/agent/quotes'
  const query = useQuery({
    queryKey: [mine ? 'my-quote' : 'quote', quoteId],
    queryFn: () => (mine ? getMyQuote(quoteId) : getQuote(quoteId)),
  })

  if (query.isPending) {
    return <Loader aria-label="Loading quote" />
  }
  if (query.isError) {
    const summary = describeError(query.error, { QUOTE_NOT_FOUND: 'Quote not found' })
    return (
      <Stack>
        <Alert color="red" title={summary.message}>
          {summary.correlationId && <Text size="xs">Reference: {summary.correlationId}</Text>}
        </Alert>
        <Anchor component={Link} to={backTo}>
          Back to quotes
        </Anchor>
      </Stack>
    )
  }

  const quote = query.data
  const hasTerms = quote.policyTermYears != null
  return (
    <Stack maw={mine ? undefined : 900} gap={mine ? 'xl' : undefined}>
      {mine && <MineHero quote={quote} backTo={backTo} />}
      {!mine && COVER_PHOTOS[quote.coverageType] && (
        <CoverPhoto
          src={COVER_PHOTOS[quote.coverageType].banner}
          alt={COVER_PHOTOS[quote.coverageType].alt}
          height={130}
          position={COVER_PHOTOS[quote.coverageType].position}
          radius="var(--mantine-radius-lg)"
        />
      )}
      {!mine && (
        <PageHeader
          icon={IconFileText}
          eyebrow="Quote service"
          title={`Quote ${quote.quoteNumber}`}
          actions={
            <Group gap="sm">
              {quote.status && <Status status={quote.status} />}
              <RefLabel label="Full ID" id={quote.quoteId} />
            </Group>
          }
        />
      )}
      <SimpleGrid cols={{ base: 1, md: 2 }}>
        <Section title="Cover">
          <DetailRow label="Customer" value={quote.customerName} />
          {quote.customerNumber && <DetailRow label="Customer ID" value={quote.customerNumber} />}
          <DetailRow label="Coverage type" value={<CoverageBadge type={quote.coverageType} />} />
          <DetailRow label="Age" value={String(quote.age)} />
          <DetailRow label="Coverage amount" value={formatMoney(quote.coverageAmount)} />
          {quote.deductible != null && <DetailRow label="Deductible" value={formatMoney(quote.deductible)} />}
        </Section>
        <Premium quote={quote} />
        <Section title={`${coverageStyle(quote.coverageType).label} details`}>
          {describeDetails(quote.coverageType, quote.details).map((row) => (
            <DetailRow key={row.label} label={row.label} value={row.value} />
          ))}
          {describeDetails(quote.coverageType, quote.details).length === 0 && <Text c="dimmed">Not recorded</Text>}
        </Section>
        {hasTerms && (
          <Section title="Policy terms">
            <DetailRow
              label="Term"
              value={`${quote.policyTermYears} ${quote.policyTermYears === 1 ? 'year' : 'years'}`}
            />
            {quote.paymentFrequency && (
              <DetailRow
                label="Payment frequency"
                value={quote.paymentFrequency.charAt(0) + quote.paymentFrequency.slice(1).toLowerCase()}
              />
            )}
            {quote.startDate && <DetailRow label="Start date" value={quote.startDate} />}
            {quote.validUntil && <DetailRow label="Valid until" value={quote.validUntil} />}
          </Section>
        )}
        {(quote.createdAt || quote.createdBy) && (
          <Section title="Record">
            {quote.createdBy && <DetailRow label="Created by" value={quote.createdBy} />}
            {quote.createdAt && <DetailRow label="Created" value={new Date(quote.createdAt).toLocaleString()} />}
          </Section>
        )}
      </SimpleGrid>
      {!mine && (
        <Group>
          <Anchor component={Link} to={backTo}>
            Back to quotes
          </Anchor>
        </Group>
      )}
    </Stack>
  )
}
