import { Card, Group, Stack, Text } from '@mantine/core'
import type { ReactNode } from 'react'

export interface MobileCardProps {
  title: ReactNode
  badge?: ReactNode
  lines: ReactNode[]
  footer?: ReactNode
}

// One record as a compact card, used instead of a wide table on phone-sized screens.
export function MobileCard({ title, badge, lines, footer }: MobileCardProps) {
  return (
    <Card padding="md">
      <Stack gap={6}>
        <Group justify="space-between" wrap="nowrap" align="center">
          {title}
          {badge}
        </Group>
        {lines.map((line, index) => (
          <Text key={index} size="sm" c={index === 0 ? undefined : 'dimmed'} fw={index === 0 ? 600 : undefined}>
            {line}
          </Text>
        ))}
        {footer}
      </Stack>
    </Card>
  )
}
