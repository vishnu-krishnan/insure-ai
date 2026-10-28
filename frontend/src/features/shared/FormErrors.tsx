import { Alert, Text } from '@mantine/core'

import type { ErrorSummary } from './errors'

export function FormErrors({ error }: { error: ErrorSummary | null }) {
  if (!error) {
    return null
  }
  return (
    <Alert color="red" title="We could not save this" role="alert">
      {error.message}
      {error.correlationId && (
        <Text size="xs" mt={4}>
          Reference: {error.correlationId}
        </Text>
      )}
    </Alert>
  )
}
