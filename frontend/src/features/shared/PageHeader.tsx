import { Box, Group, Stack, Text, ThemeIcon, Title } from '@mantine/core'
import type { Icon } from '@tabler/icons-react'
import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  description?: string
  actions?: ReactNode
  icon?: Icon
  eyebrow?: string
}

export function PageHeader({ title, description, actions, icon: Glyph, eyebrow }: PageHeaderProps) {
  return (
    <Box
      className="fade-up"
      p={{ base: 'md', sm: 'lg' }}
      mb="xs"
      style={{
        borderRadius: 'var(--mantine-radius-lg)',
        border: '1px solid var(--mantine-color-default-border)',
        background:
          'linear-gradient(120deg, light-dark(var(--mantine-color-brand-0), var(--mantine-color-dark-6)), light-dark(white, var(--mantine-color-dark-7)))',
      }}
    >
      <Group justify="space-between" align="center" wrap="wrap" gap="sm">
        <Group gap="md" wrap="nowrap" style={{ minWidth: 0 }}>
          {Glyph && (
            <ThemeIcon
              size={48}
              radius="lg"
              variant="gradient"
              gradient={{ from: 'brand.7', to: 'brand.4', deg: 135 }}
              visibleFrom="xs"
            >
              <Glyph size={26} stroke={1.6} />
            </ThemeIcon>
          )}
          <Stack gap={2} style={{ minWidth: 0 }}>
            {eyebrow && (
              <Text size="xs" fw={650} tt="uppercase" c="brand" style={{ letterSpacing: 0.6 }}>
                {eyebrow}
              </Text>
            )}
            <Title order={2} fz={{ base: 22, sm: 28 }}>
              {title}
            </Title>
            {description && (
              <Text c="dimmed" fz={{ base: 'sm', sm: 'md' }}>
                {description}
              </Text>
            )}
          </Stack>
        </Group>
        {actions}
      </Group>
    </Box>
  )
}
