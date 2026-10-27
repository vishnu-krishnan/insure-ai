import {
  Accordion,
  Anchor,
  Badge,
  Box,
  Button,
  Card,
  Container,
  Group,
  List,
  NumberInput,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import { IconArrowRight, IconCheck, IconX } from '@tabler/icons-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { Reveal } from '../../app/Reveal'
import { useAuth } from '../../auth/useAuth'
import { formatMoney } from '../quotes/format'
import { coverageStyle } from '../shared/coverage'
import { COVER_GUIDES, findGuide } from './catalogue'
import { estimatePremium } from './estimate'

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card p={{ base: 'md', sm: 'xl' }} h="100%">
      <Stack gap="sm">
        <Title order={3} fz="lg">
          {title}
        </Title>
        {children}
      </Stack>
    </Card>
  )
}

function Estimator({ code }: { code: string }) {
  const { signIn, status } = useAuth()
  const [cover, setCover] = useState<number | string>(250000)
  const [age, setAge] = useState<number | string>(36)
  const estimate = typeof cover === 'number' && typeof age === 'number' ? estimatePremium(code, cover, age) : null

  return (
    <Card p={{ base: 'md', sm: 'xl' }} style={{ position: 'sticky', top: 84 }}>
      <Stack gap="md">
        <Stack gap={2}>
          <Title order={3} fz="lg">
            Estimate your premium
          </Title>
          <Text size="sm" c="dimmed">
            An indicative figure before tax. Your quote is the final price.
          </Text>
        </Stack>
        <NumberInput label="Cover amount" value={cover} onChange={setCover} min={0} thousandSeparator="," />
        <NumberInput label="Age" value={age} onChange={setAge} min={18} max={75} allowDecimal={false} />
        <Box
          p="md"
          style={{
            borderRadius: 'var(--mantine-radius-md)',
            color: 'white',
            background: 'linear-gradient(135deg, var(--mantine-color-brand-9), var(--mantine-color-brand-6))',
          }}
        >
          {estimate ? (
            <SimpleGrid cols={2}>
              <Stack gap={2}>
                <Text size="sm" c="white" opacity={0.8}>
                  A year
                </Text>
                <Text fw={700} fz={24} c="white">
                  {formatMoney(estimate.annual)}
                </Text>
              </Stack>
              <Stack gap={2}>
                <Text size="sm" c="white" opacity={0.8}>
                  A month
                </Text>
                <Text fw={700} fz={24} c="white">
                  {formatMoney(estimate.monthly)}
                </Text>
              </Stack>
            </SimpleGrid>
          ) : (
            <Text c="white">Enter a cover amount and an age from 18 to 75</Text>
          )}
        </Box>
        {status !== 'authenticated' && (
          <Button onClick={() => void signIn('/agent/quotes/new')} rightSection={<IconArrowRight size={16} />}>
            Agent sign in to quote
          </Button>
        )}
        <Anchor component={Link} to="/calculator" size="sm">
          Open the calculator
        </Anchor>
      </Stack>
    </Card>
  )
}

export function InsuranceDetailPage() {
  const { type } = useParams()
  const guide = findGuide(type)

  if (!guide) {
    return (
      <Container size={640} py={80}>
        <Stack align="center" gap="sm" ta="center">
          <Title order={2}>We do not offer that cover type</Title>
          <Text c="dimmed">Choose one of the types below.</Text>
          <Button component={Link} to="/insurance">
            See all insurance plans
          </Button>
        </Stack>
      </Container>
    )
  }

  const style = coverageStyle(guide.code)
  const others = COVER_GUIDES.filter((item) => item.code !== guide.code)

  return (
    <>
      <Box style={{ color: 'white', background: 'linear-gradient(135deg, #0f275f, #1b45ae 60%, #2557d6)' }}>
        <Container size={1160} py={{ base: 40, sm: 72 }}>
          <Group wrap="nowrap" gap="xl" align="center">
            <ThemeIcon size={88} radius="xl" variant="white" color={style.color} className="float" visibleFrom="sm">
              <style.icon size={48} stroke={1.5} />
            </ThemeIcon>
            <Stack gap="sm" maw={700}>
              <Badge variant="white" color="brand" size="lg" w="fit-content">
                Insurance plan
              </Badge>
              <Title order={1} c="white" fz={{ base: 32, sm: 46 }} lh={1.1}>
                {guide.name}
              </Title>
              <Text c="white" opacity={0.92} fz={{ base: 'md', sm: 'lg' }}>
                {guide.summary}
              </Text>
            </Stack>
          </Group>
        </Container>
      </Box>

      <Container size={1160} py={{ base: 32, sm: 56 }}>
        <SimpleGrid cols={{ base: 1, md: 3 }} spacing="xl">
          <Stack gap="lg" style={{ gridColumn: 'span 2' }}>
            <Reveal>
              <Panel title="Who it suits">
                <List
                  spacing={6}
                  icon={
                    <ThemeIcon size={20} radius="xl" color={style.color} variant="light">
                      <IconCheck size={12} stroke={3} />
                    </ThemeIcon>
                  }
                >
                  {guide.whoFor.map((item) => (
                    <List.Item key={item}>{item}</List.Item>
                  ))}
                </List>
              </Panel>
            </Reveal>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="lg">
              <Reveal delay={80}>
                <Panel title="What's covered">
                  <List
                    spacing={6}
                    icon={
                      <ThemeIcon size={20} radius="xl" color="teal" variant="light">
                        <IconCheck size={12} stroke={3} />
                      </ThemeIcon>
                    }
                  >
                    {guide.covered.map((item) => (
                      <List.Item key={item}>{item}</List.Item>
                    ))}
                  </List>
                </Panel>
              </Reveal>
              <Reveal delay={160}>
                <Panel title="What's not covered">
                  <List
                    spacing={6}
                    icon={
                      <ThemeIcon size={20} radius="xl" color="red" variant="light">
                        <IconX size={12} stroke={3} />
                      </ThemeIcon>
                    }
                  >
                    {guide.notCovered.map((item) => (
                      <List.Item key={item}>{item}</List.Item>
                    ))}
                  </List>
                </Panel>
              </Reveal>
            </SimpleGrid>
            <Reveal>
              <Panel title="Good to know">
                <Text size="sm" c="dimmed">
                  Premium basis: {guide.premiumBasis}. It rises by 15% for customers over 60, and cover is offered from
                  age 18 to 75. Policies run for 1 to 40 years and can be paid monthly, quarterly or yearly.
                </Text>
                <Accordion variant="separated" radius="md" defaultValue={null}>
                  {guide.faqs.map((item) => (
                    <Accordion.Item key={item.q} value={item.q}>
                      <Accordion.Control>{item.q}</Accordion.Control>
                      <Accordion.Panel>
                        <Text c="dimmed">{item.a}</Text>
                      </Accordion.Panel>
                    </Accordion.Item>
                  ))}
                </Accordion>
              </Panel>
            </Reveal>
          </Stack>
          <Estimator code={guide.code} />
        </SimpleGrid>

        <Stack gap="md" mt={56}>
          <Title order={2} fz={{ base: 22, sm: 28 }}>
            Other cover types
          </Title>
          <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
            {others.map((item) => {
              const other = coverageStyle(item.code)
              return (
                <Card
                  key={item.code}
                  className="lift-soft"
                  component={Link}
                  to={`/insurance/${item.slug}`}
                  p="md"
                  style={{ textDecoration: 'none', color: 'inherit' }}
                >
                  <Group wrap="nowrap">
                    <ThemeIcon size={40} radius="md" variant="light" color={other.color}>
                      <other.icon size={22} stroke={1.6} />
                    </ThemeIcon>
                    <Stack gap={0}>
                      <Text fw={600}>{item.name}</Text>
                      <Text size="sm" c="dimmed">
                        {item.tagline}
                      </Text>
                    </Stack>
                  </Group>
                </Card>
              )
            })}
          </SimpleGrid>
        </Stack>
      </Container>
    </>
  )
}
