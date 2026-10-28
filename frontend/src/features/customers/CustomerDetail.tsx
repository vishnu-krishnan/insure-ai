import { Alert, Anchor, Button, Card, Group, Loader, SimpleGrid, Stack, Text, ThemeIcon } from '@mantine/core'
import { IconPlus, IconUser } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'

import { getCustomer } from '../../api/customers'
import { listQuotes } from '../../api/quotes'
import type { Customer } from '../../api/schemas'
import { COVER_GUIDES } from '../insurance/catalogue'
import { portalPath } from '../portal/paths'
import { coverageStyle } from '../shared/coverage'
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

function CustomerQuotes({ customerId, customerNumber }: { customerId: string; customerNumber: string }) {
  const query = useQuery({
    queryKey: ['quotes', 'customer', customerNumber],
    queryFn: () => listQuotes(0, 100, customerNumber),
  })

  return (
    <Section title="Insurance">
      {query.isPending && <Loader size="sm" aria-label="Loading quotes" />}
      {query.isError && <Text c="dimmed">Quotes could not be loaded</Text>}
      {query.data && (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }} spacing="md">
          {COVER_GUIDES.map((guide) => {
            const style = coverageStyle(guide.code)
            const quotes = query.data.content.filter((quote) => quote.coverageType === guide.code)
            return (
              <Card key={guide.code} withBorder p="md" radius="md" className="lift-soft">
                <Stack gap="sm">
                  <Group gap="sm" wrap="nowrap">
                    <ThemeIcon size={36} radius="md" variant="light" color={style.color}>
                      <style.icon size={20} />
                    </ThemeIcon>
                    <Stack gap={0}>
                      <Text fw={650}>{style.label}</Text>
                      <Text size="xs" c="dimmed">
                        {quotes.length === 0
                          ? 'No quotes yet'
                          : `${quotes.length} ${quotes.length === 1 ? 'quote' : 'quotes'}`}
                      </Text>
                    </Stack>
                  </Group>
                  {quotes.slice(0, 3).map((quote) => (
                    <Group key={quote.quoteId} justify="space-between" wrap="nowrap">
                      <Anchor component={Link} to={`/agent/quotes/${quote.quoteId}`} size="sm">
                        {quote.quoteNumber}
                      </Anchor>
                      <Text size="xs" c="dimmed">
                        {formatMoney(quote.annualPremium)} a year
                      </Text>
                    </Group>
                  ))}
                  <Button
                    component={Link}
                    to={`${portalPath(guide.code)}/new?customerId=${customerId}`}
                    variant="light"
                    color={style.color}
                    size="xs"
                    leftSection={<IconPlus size={14} />}
                  >
                    {`New ${style.label.toLowerCase()} quote`}
                  </Button>
                </Stack>
              </Card>
            )
          })}
        </SimpleGrid>
      )}
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
        description="Everything on file for this customer, with their quotes grouped by type of cover."
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
      <CustomerQuotes customerId={customer.customerId} customerNumber={customer.customerNumber} />
      {customer.createdAt && (
        <Text size="xs" c="dimmed">
          Created {new Date(customer.createdAt).toLocaleString()}
        </Text>
      )}
      <Group>
        <Anchor component={Link} to="/agent/customers">
          Back to customers
        </Anchor>
      </Group>
    </Stack>
  )
}
