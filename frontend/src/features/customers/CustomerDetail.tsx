import { Alert, Anchor, Button, Card, Group, Loader, SimpleGrid, Stack, Text } from '@mantine/core'
import { IconUser } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'

import { getCustomer } from '../../api/customers'
import { listQuotes } from '../../api/quotes'
import type { Customer } from '../../api/schemas'
import { CoverageBadge } from '../shared/CoverageBadge'
import { DetailRow } from '../shared/DetailRow'
import { PageHeader } from '../shared/PageHeader'
import { RefLabel } from '../shared/RefLabel'
import { describeError } from '../shared/errors'
import { formatMoney } from '../quotes/format'
import { labelFor } from './labels'

const NOT_STATED = 'Not stated'

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

function Address({ customer }: { customer: Customer }) {
  const address = customer.address
  if (!address) {
    return <Text c="dimmed">No address recorded</Text>
  }
  return (
    <Stack gap={2}>
      <Text>{address.line1}</Text>
      {address.line2 && <Text>{address.line2}</Text>}
      <Text>{`${address.city}, ${address.state} ${address.postalCode}`}</Text>
      <Text>{address.country}</Text>
    </Stack>
  )
}

function CustomerQuotes({ customerNumber }: { customerNumber: string }) {
  const query = useQuery({
    queryKey: ['quotes', 'customer', customerNumber],
    queryFn: () => listQuotes(0, 5, customerNumber),
  })

  return (
    <Section title="Quotes">
      {query.isPending && <Loader size="sm" aria-label="Loading quotes" />}
      {query.isError && <Text c="dimmed">Quotes could not be loaded</Text>}
      {query.data && query.data.content.length === 0 && <Text c="dimmed">No quotes for this customer yet</Text>}
      {query.data?.content.map((quote) => (
        <Group key={quote.quoteId} justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Anchor component={Link} to={`/agent/quotes/${quote.quoteId}`}>
              {quote.quoteNumber}
            </Anchor>
            <CoverageBadge type={quote.coverageType} />
          </Group>
          <Text size="sm">{formatMoney(quote.annualPremium)} a year</Text>
        </Group>
      ))}
    </Section>
  )
}

export function CustomerDetail() {
  const { customerId = '' } = useParams()
  const query = useQuery({ queryKey: ['customer', customerId], queryFn: () => getCustomer(customerId) })

  if (query.isPending) {
    return <Loader aria-label="Loading customer" />
  }
  if (query.isError) {
    const summary = describeError(query.error, { CUSTOMER_NOT_FOUND: 'Customer not found' })
    return (
      <Stack>
        <Alert color="red" title={summary.message}>
          {summary.correlationId && <Text size="xs">Reference: {summary.correlationId}</Text>}
        </Alert>
        <Anchor component={Link} to="/agent/customers">
          Back to customers
        </Anchor>
      </Stack>
    )
  }

  const customer = query.data
  return (
    <Stack maw={900}>
      <PageHeader
        icon={IconUser}
        eyebrow="Customer service"
        title={customer.fullName}
        description="Customer profile"
        actions={<RefLabel label={customer.customerNumber} id={customer.customerId} />}
      />
      <SimpleGrid cols={{ base: 1, md: 2 }}>
        <Section title="Personal details">
          <DetailRow label="Date of birth" value={customer.dateOfBirth} />
          <DetailRow label="Gender" value={labelFor(customer.gender) || NOT_STATED} />
          <DetailRow label="Marital status" value={labelFor(customer.maritalStatus) || NOT_STATED} />
          <DetailRow label="Occupation" value={customer.occupation || NOT_STATED} />
          <DetailRow
            label="Annual income"
            value={customer.annualIncome == null ? NOT_STATED : formatMoney(customer.annualIncome)}
          />
        </Section>
        <Section title="Contact">
          <DetailRow label="Email" value={customer.email} />
          <DetailRow label="Phone" value={customer.phone} />
        </Section>
        <Section title="Address">
          <Address customer={customer} />
        </Section>
        <Section title="Nominee">
          {customer.nominee ? (
            <>
              <DetailRow label="Name" value={customer.nominee.name} />
              <DetailRow label="Relationship" value={customer.nominee.relationship} />
            </>
          ) : (
            <Text c="dimmed">No nominee recorded</Text>
          )}
        </Section>
      </SimpleGrid>
      <CustomerQuotes customerNumber={customer.customerNumber} />
      {customer.createdAt && (
        <Text size="xs" c="dimmed">
          Created {new Date(customer.createdAt).toLocaleString()}
        </Text>
      )}
      <Group>
        <Button component={Link} to={`/agent/quotes/new?customerId=${customer.customerId}`}>
          Create a quote
        </Button>
        <Anchor component={Link} to="/agent/customers">
          Back to customers
        </Anchor>
      </Group>
    </Stack>
  )
}
