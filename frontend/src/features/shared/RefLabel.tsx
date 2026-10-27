import { ActionIcon, Anchor, CopyButton, Group, Text, Tooltip } from '@mantine/core'
import { IconCheck, IconCopy } from '@tabler/icons-react'
import { Link } from 'react-router-dom'

// A short reference with the full ID on hover and a copy button for it. With `to` the label is a link.
export function RefLabel({ label, id, to }: { label: string; id: string; to?: string }) {
  return (
    <Group gap={4} wrap="nowrap">
      <Tooltip label={id} openDelay={200}>
        {to ? (
          <Anchor component={Link} to={to} ff="monospace" size="sm" fw={500}>
            {label}
          </Anchor>
        ) : (
          <Text span ff="monospace" size="sm" fw={500}>
            {label}
          </Text>
        )}
      </Tooltip>
      <CopyButton value={id} timeout={1500}>
        {({ copied, copy }) => (
          <ActionIcon
            variant="subtle"
            color={copied ? 'teal' : 'gray'}
            size="sm"
            onClick={copy}
            aria-label={copied ? 'Copied' : `Copy full ID of ${label}`}
          >
            {copied ? <IconCheck size={14} /> : <IconCopy size={14} />}
          </ActionIcon>
        )}
      </CopyButton>
    </Group>
  )
}
