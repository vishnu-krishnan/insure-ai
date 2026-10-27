import { Badge } from '@mantine/core'

import { coverageStyle } from './coverage'

export function CoverageBadge({ type }: { type: string }) {
  const { color, icon: Glyph } = coverageStyle(type)
  return (
    <Badge variant="light" color={color} radius="xl" leftSection={<Glyph size={12} stroke={2} />}>
      {type}
    </Badge>
  )
}
