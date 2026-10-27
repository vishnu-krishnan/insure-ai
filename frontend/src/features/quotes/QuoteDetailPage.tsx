import { Alert, Anchor, Badge, Card, Divider, Group, Loader, SimpleGrid, Stack, Text } from '@mantine/core'
import { IconFileText } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'

import { getQuote } from '../../api/quotes'
import type { QuoteSummary } from '../../api/schemas'
import { CoverageBadge } from '../shared/CoverageBadge'
import { DetailRow } from '../shared/DetailRow'
import { PageHeader } from '../shared/PageHeader'
import { RefLabel } from '../shared/RefLabel'
import { describeError } from '../shared/errors'
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

export function QuoteDetailPage() {
  const { quoteId = '' } = useParams()
  const query = useQuery({ queryKey: ['quote', quoteId], queryFn: () => getQuote(quoteId) })

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
        <Anchor component={Link} to="/agent/quotes">
          Back to quotes
        </Anchor>
      </Stack>
    )
  }

  const quote = query.data
  const hasTerms = quote.policyTermYears != null
  return (
    <Stack maw={900}>
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
      <Group>
        <Anchor component={Link} to="/agent/quotes">
          Back to quotes
        </Anchor>
      </Group>
    </Stack>
  )
}
