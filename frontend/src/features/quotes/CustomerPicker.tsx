import { Alert, Button, Card, Group, Loader, Stack, Text, TextInput, UnstyledButton } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconSearch } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'

import { getCustomer, searchCustomers } from '../../api/customers'
import type { Customer } from '../../api/schemas'
import { describeError } from '../shared/errors'

interface Props {
  initialId?: string
  error?: string
  onChange(customer: Customer | null): void
}

export function CustomerPicker({ initialId = '', error, onChange }: Props) {
  const [selected, setSelected] = useState<Customer | null>(null)
  const [typed, setTyped] = useState('')
  const [debounced] = useDebouncedValue(typed, 250)
  const preselected = useRef(false)
  const [preselectError, setPreselectError] = useState<string | null>(null)

  useEffect(() => {
    if (!initialId || preselected.current) {
      return
    }
    preselected.current = true
    getCustomer(initialId.trim())
      .then((customer) => choose(customer))
      .catch((failure: unknown) =>
        setPreselectError(describeError(failure, { CUSTOMER_NOT_FOUND: 'Customer not found' }).message),
      )
    // Runs once for the customer named in the address.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const results = useQuery({
    queryKey: ['customers', 'picker', debounced],
    queryFn: () => searchCustomers(debounced, 0, 5),
    enabled: selected === null && debounced.trim() !== '',
  })

  function choose(customer: Customer | null) {
    setSelected(customer)
    onChange(customer)
  }

  if (selected) {
    return (
      <Stack gap="xs">
        <Text fw={500} size="sm">
          Selected customer
        </Text>
        <Card padding="md">
          <Group justify="space-between" wrap="nowrap">
            <Stack gap={0}>
              <Text fw={600}>{selected.fullName}</Text>
              <Text size="sm" c="dimmed">
                {selected.customerNumber} · {selected.email}
              </Text>
            </Stack>
            <Button variant="default" size="xs" onClick={() => choose(null)}>
              Change
            </Button>
          </Group>
        </Card>
      </Stack>
    )
  }

  const searching = debounced.trim() !== ''
  const message = error ?? preselectError ?? undefined
  return (
    <Stack gap="xs">
      <TextInput
        label="Find a customer"
        placeholder="Name, CUS-000001, email or phone"
        leftSection={<IconSearch size={16} />}
        value={typed}
        error={message}
        onChange={(event) => setTyped(event.currentTarget.value)}
      />
      {!searching && (
        <Text size="sm" c="dimmed">
          Start typing to find a customer. Or open a customer and choose a cover from there.
        </Text>
      )}
      {searching && results.isPending && <Loader size="xs" aria-label="Searching customers" />}
      {searching && results.isError && <Alert color="red" title={describeError(results.error).message} />}
      {searching && results.data && results.data.content.length === 0 && (
        <Text size="sm" c="dimmed">
          No customers match your search
        </Text>
      )}
      {searching &&
        results.data?.content.map((customer) => (
          <UnstyledButton key={customer.customerId} onClick={() => choose(customer)}>
            <Card padding="sm" className="lift-soft">
              <Text fw={600}>{customer.fullName}</Text>
              <Text size="sm" c="dimmed">
                {customer.customerNumber} · {customer.email}
              </Text>
            </Card>
          </UnstyledButton>
        ))}
    </Stack>
  )
}
