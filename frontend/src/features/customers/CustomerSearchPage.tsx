import { Alert, Button, Card, Group, Loader, NativeSelect, Stack, Table, Text, TextInput } from '@mantine/core'
import { useDebouncedValue, useMediaQuery } from '@mantine/hooks'
import { IconPlus, IconSearch, IconUsers } from '@tabler/icons-react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { searchCustomers } from '../../api/customers'
import { describeError } from '../shared/errors'
import { MobileCard } from '../shared/MobileCards'
import { PageHeader } from '../shared/PageHeader'
import { RefLabel } from '../shared/RefLabel'

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

export function CustomerSearchPage() {
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
    queryKey: ['customers', text, page, size],
    queryFn: () => searchCustomers(text, page, size),
    placeholderData: keepPreviousData,
  })

  function go(nextPage: number, nextSize = size) {
    setParams({ ...(text ? { q: text } : {}), page: String(nextPage), size: String(nextSize) })
  }

  const header = (
    <PageHeader
      icon={IconUsers}
      eyebrow="Customer service"
      title="Customers"
      description="Find a policyholder by name, customer ID, email or phone, then open their record to see every policy."
      actions={
        <Button component={Link} to="/agent/customers/new" leftSection={<IconPlus size={16} />}>
          New customer
        </Button>
      }
    />
  )
  const search = (
    <TextInput
      label="Search customers"
      placeholder="Name, CUS-000001, email or phone"
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
        <Loader aria-label="Loading customers" />
      </Stack>
    )
  }

  const { content, totalElements, totalPages } = query.data
  const pastEnd = content.length === 0 && totalPages > 0 && page >= totalPages

  return (
    <Stack>
      {header}
      {search}
      {pastEnd ? (
        <Stack align="flex-start">
          <Text>That page does not exist.</Text>
          <Button onClick={() => go(totalPages - 1)}>Go to the last page</Button>
        </Stack>
      ) : content.length === 0 ? (
        <Text>{text ? 'No customers match your search' : 'No customers yet'}</Text>
      ) : phone ? (
        <Stack gap="sm">
          {content.map((customer) => (
            <MobileCard
              key={customer.customerId}
              title={
                <RefLabel
                  label={customer.customerNumber}
                  id={customer.customerId}
                  to={`/agent/customers/${customer.customerId}`}
                />
              }
              lines={[customer.fullName, customer.email, customer.phone]}
            />
          ))}
        </Stack>
      ) : (
        <Card padding={0} style={{ overflow: 'hidden' }}>
          <Table.ScrollContainer minWidth={640}>
            <Table highlightOnHover>
              <Table.Thead bg="light-dark(var(--mantine-color-brand-0), var(--mantine-color-dark-6))">
                <Table.Tr>
                  <Table.Th>Customer ID</Table.Th>
                  <Table.Th>Name</Table.Th>
                  <Table.Th>Email</Table.Th>
                  <Table.Th>Phone</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {content.map((customer) => (
                  <Table.Tr key={customer.customerId}>
                    <Table.Td>
                      <RefLabel
                        label={customer.customerNumber}
                        id={customer.customerId}
                        to={`/agent/customers/${customer.customerId}`}
                      />
                    </Table.Td>
                    <Table.Td>{customer.fullName}</Table.Td>
                    <Table.Td>{customer.email}</Table.Td>
                    <Table.Td>{customer.phone}</Table.Td>
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
          <Text>{`Page ${page + 1} of ${Math.max(totalPages, 1)}`}</Text>
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
          <Text
            size="sm"
            c="dimmed"
          >{`${totalElements} ${totalElements === 1 ? 'customer' : 'customers'} in total`}</Text>
          <NativeSelect
            aria-label="Customers per page"
            data={PAGE_SIZES.map(String)}
            value={String(size)}
            onChange={(event) => go(0, Number(event.currentTarget.value))}
          />
        </Group>
      </Group>
    </Stack>
  )
}
