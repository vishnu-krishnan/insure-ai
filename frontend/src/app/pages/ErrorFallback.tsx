import { Button, Group, Stack, Text, Title } from '@mantine/core'

export function ErrorFallback() {
  return (
    <Stack p="md">
      <Title order={2}>Something went wrong</Title>
      <Text>The page could not be displayed. Try again, or return to the start.</Text>
      <Group>
        <Button onClick={() => window.location.reload()}>Try again</Button>
        <Button variant="default" component="a" href="/">
          Go to the start
        </Button>
      </Group>
    </Stack>
  )
}
