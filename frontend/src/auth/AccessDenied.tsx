import { Alert } from '@mantine/core'

export function AccessDenied() {
  return (
    <Alert color="red" title="Access denied" m="md">
      Your account does not have permission to view this page.
    </Alert>
  )
}
