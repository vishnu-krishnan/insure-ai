import { Box, Group, Stack, Text, Title } from '@mantine/core'
import type { ReactNode } from 'react'

interface MeHeaderProps {
  eyebrow?: string
  title: string
  description?: string
  actions?: ReactNode
}

export function MeHeader({ eyebrow, title, description, actions }: MeHeaderProps) {
  return (
    <Group className="fade-up" justify="space-between" align="flex-end" wrap="wrap" gap="md">
      <Stack gap={4} style={{ minWidth: 0 }}>
        {eyebrow && (
          <Text size="xs" fw={700} tt="uppercase" c="brand" style={{ letterSpacing: 1 }}>
            {eyebrow}
          </Text>
        )}
        <Title order={1} fz={{ base: 26, sm: 32 }} fw={800} lh={1.15} style={{ letterSpacing: -0.5 }}>
          {title}
        </Title>
        {description && (
          <Text c="dimmed" maw={620} fz={{ base: 'sm', sm: 'md' }}>
            {description}
          </Text>
        )}
      </Stack>
      {actions && <Box>{actions}</Box>}
    </Group>
  )
}
