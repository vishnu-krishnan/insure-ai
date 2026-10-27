import { Anchor, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router-dom'

export function NotFound() {
  return (
    <Stack p="md">
      <Title order={2}>Page not found</Title>
      <Text>The page you are looking for does not exist.</Text>
      <Anchor component={Link} to="/">
        Back to the start
      </Anchor>
    </Stack>
  )
}
