import { Alert, Button, Card, Group, Loader, NativeSelect, Stack, Table, Text, TextInput } from '@mantine/core'
import { useDebouncedValue, useMediaQuery } from '@mantine/hooks'
import { IconFileText, IconPlus, IconSearch } from '@tabler/icons-react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { listQuotes } from '../../api/quotes'
import { coverageStyle } from '../shared/coverage'
import { portalPath } from '../portal/paths'
import { describeDetails } from '../portal/typeFields'
import { CoverageBadge } from '../shared/CoverageBadge'
import { MobileCard } from '../shared/MobileCards'
import { PageHeader } from '../shared/PageHeader'
import { RefLabel } from '../shared/RefLabel'
import { describeError } from '../shared/errors'
import { formatMoney } from './format'

const PAGE_SIZES = [10, 20, 50, 100]
const DEFAULT_SIZE = 20

function readPage(value: string | null): number {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0
}

function readSize(value: string | null): number {
  const parsed = Number(value)
  return PAGE_SIZES.includes(parsed) ? parsed : DEFAULT_SIZE
}

function summarise(type: string, details: Record<string, unknown> | null | undefined): string {
  const rows = describeDetails(type, details)
  return rows.length === 0
    ? 'Not recorded'
    : rows
        .slice(0, 3)
        .map((row) => row.value)
        .join(' · ')
}

export function QuoteListPage({ type }: { type?: string } = {}) {
  const [params, setParams] = useSearchParams()
  const text = params.get('q') ?? ''
  const page = readPage(params.get('page'))
  const size = readSize(params.get('size'))
  const phone = useMediaQuery('(max-width: 48em)')
  const [typed, setTyped] = useState(text)
  const [debounced] = useDebouncedValue(typed, 300)

  useEffect(() => {
    if (debounced !== text) {
      setParams(debounced ? { q: debounced, page: '0', size: String(size) } : { page: '0', size: String(size) })
    }
    // Only the debounced text should drive the URL.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debounced])

  const query = useQuery({
    queryKey: ['quotes', type ?? '', text, page, size],
    queryFn: () => listQuotes(page, size, text, type ?? ''),
    placeholderData: keepPreviousData,
  })

  function go(nextPage: number, nextSize = size) {
    setParams({ ...(text ? { q: text } : {}), page: String(nextPage), size: String(nextSize) })
  }

  const header = (
    <PageHeader
      icon={type ? coverageStyle(type).icon : IconFileText}
      eyebrow={type ? 'Insurance portal' : 'Quote service'}
      title={type ? `${coverageStyle(type).label} insurance` : 'Quotes'}
      description={
        type
          ? `Every ${coverageStyle(type).label.toLowerCase()} quote, newest first.`
          : 'Every quote created, newest first.'
      }
      actions={
        <Button
          component={Link}
          to={type ? `${portalPath(type)}/new` : '/agent/quotes/new'}
          leftSection={<IconPlus size={16} />}
        >
          New quote
        </Button>
      }
    />
  )
  const search = (
    <TextInput
      label="Search quotes"
      placeholder="Quote ID, customer name, customer ID or coverage"
      leftSection={<IconSearch size={16} />}
      value={typed}
      onChange={(event) => setTyped(event.currentTarget.value)}
      maxLength={80}
    />
  )

  if (query.isError) {
    const summary = describeError(query.error)
    return (
      <Stack>
        {header}
        {search}
        <Alert color="red" title={summary.message}>
          {summary.correlationId && <Text size="xs">Reference: {summary.correlationId}</Text>}
        </Alert>
      </Stack>
    )
  }
  if (query.isPending) {
    return (
      <Stack>
        {header}
        {search}
        <Loader aria-label="Loading quotes" />
      </Stack>
    )
  }

  const { content, totalElements, totalPages } = query.data
  const pastEnd = content.length === 0 && totalPages > 0 && page >= totalPages

  return (
    <Stack>
      {header}
      {search}
      {totalElements === 0 ? (
        <Text>
          {text
            ? 'No quotes match your search'
            : type
              ? `No ${coverageStyle(type).label.toLowerCase()} quotes yet`
              : 'No quotes yet'}
        </Text>
      ) : pastEnd ? (
        <Stack align="flex-start">
          <Text>That page does not exist.</Text>
          <Button onClick={() => go(totalPages - 1)}>Go to the last page</Button>
        </Stack>
      ) : phone ? (
        <Stack gap="sm">
          {content.map((quote) => (
            <MobileCard
              key={quote.quoteId}
              title={<RefLabel label={quote.quoteNumber} id={quote.quoteId} to={`/agent/quotes/${quote.quoteId}`} />}
              badge={<CoverageBadge type={quote.coverageType} />}
              lines={[
                quote.customerName,
                `Cover ${formatMoney(quote.coverageAmount)}`,
                `Premium ${formatMoney(quote.annualPremium)} a year · ${formatMoney(quote.monthlyPremium)} a month`,
              ]}
            />
          ))}
        </Stack>
      ) : (
        <Card padding={0} style={{ overflow: 'hidden' }}>
          <Table.ScrollContainer minWidth={720}>
            <Table highlightOnHover>
              <Table.Thead bg="light-dark(var(--mantine-color-brand-0), var(--mantine-color-dark-6))">
                <Table.Tr>
                  <Table.Th>Quote</Table.Th>
                  <Table.Th>Customer</Table.Th>
                  <Table.Th>Coverage</Table.Th>
                  {type && <Table.Th>Details</Table.Th>}
                  <Table.Th ta="right">Amount</Table.Th>
                  <Table.Th ta="right">Annual</Table.Th>
                  <Table.Th ta="right">Monthly</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {content.map((quote) => (
                  <Table.Tr key={quote.quoteId}>
                    <Table.Td>
                      <RefLabel label={quote.quoteNumber} id={quote.quoteId} to={`/agent/quotes/${quote.quoteId}`} />
                    </Table.Td>
                    <Table.Td>{quote.customerName}</Table.Td>
                    <Table.Td>
                      <CoverageBadge type={quote.coverageType} />
                    </Table.Td>
                    {type && (
                      <Table.Td>
                        <Text size="sm" c="dimmed">
                          {summarise(quote.coverageType, quote.details)}
                        </Text>
                      </Table.Td>
                    )}
                    <Table.Td ta="right">{formatMoney(quote.coverageAmount)}</Table.Td>
                    <Table.Td ta="right">{formatMoney(quote.annualPremium)}</Table.Td>
                    <Table.Td ta="right">{formatMoney(quote.monthlyPremium)}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </Card>
      )}
      <Group justify="space-between">
        <Group>
          <Button variant="default" disabled={page <= 0} onClick={() => go(page - 1)} aria-label="Previous page">
            Previous
          </Button>
          <Text>{`Page ${page + 1} of ${totalPages}`}</Text>
          <Button
            variant="default"
            disabled={page + 1 >= totalPages}
            onClick={() => go(page + 1)}
            aria-label="Next page"
          >
            Next
          </Button>
        </Group>
        <Group>
          <Text size="sm" c="dimmed">{`${totalElements} ${totalElements === 1 ? 'quote' : 'quotes'} in total`}</Text>
          <NativeSelect
            aria-label="Quotes per page"
            data={PAGE_SIZES.map(String)}
            value={String(size)}
            onChange={(event) => go(0, Number(event.currentTarget.value))}
          />
        </Group>
      </Group>
    </Stack>
  )
}
