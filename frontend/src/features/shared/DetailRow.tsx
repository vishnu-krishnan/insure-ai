import { Group, Text } from '@mantine/core'
import type { ReactNode } from 'react'

export function DetailRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <Group justify="space-between" align="flex-start" wrap="nowrap" gap="md">
      <Text c="dimmed" style={{ flexShrink: 0 }}>
        {label}
      </Text>
      <Text fw={500} ta="right" style={{ minWidth: 0, overflowWrap: 'anywhere' }}>
        {value}
      </Text>
    </Group>
  )
}
