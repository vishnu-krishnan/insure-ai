import {
  Anchor,
  Badge,
  Box,
  Card,
  Container,
  Group,
  SimpleGrid,
  Stack,
  Table,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import { IconArrowRight, IconCheck } from '@tabler/icons-react'
import { Link } from 'react-router-dom'

import { Reveal } from '../../app/Reveal'
import { CoverPhoto } from '../shared/CoverPhoto'
import { coverageStyle } from '../shared/coverage'
import { COVER_GUIDES, GUIDE_QUESTIONS } from './catalogue'
import { COVER_PHOTOS } from './photos'

export function InsuranceIndexPage() {
  return (
    <>
      <Box style={{ color: 'white', background: 'linear-gradient(135deg, #0f275f, #2557d6)' }}>
        <Container size={1160} py={{ base: 40, sm: 72 }}>
          <Stack gap="md" maw={680}>
            <Badge variant="white" color="brand" size="lg" w="fit-content">
              Plans and cover
            </Badge>
            <Title order={1} c="white" fz={{ base: 32, sm: 46 }} lh={1.1}>
              Compare insurance plans
            </Title>
            <Text c="white" opacity={0.9} fz={{ base: 'md', sm: 'lg' }}>
              See what each type of cover protects, what it leaves out and roughly what it costs, then choose the one
              that fits your life.
            </Text>
          </Stack>
        </Container>
      </Box>

      <Container size={1160} py={{ base: 36, sm: 64 }}>
        <Stack gap={56}>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }} spacing="lg">
            {COVER_GUIDES.map((guide, index) => {
              const style = coverageStyle(guide.code)
              return (
                <Reveal key={guide.code} delay={index * 80}>
                  <Card
                    className="lift"
                    component={Link}
                    to={`/insurance/${guide.slug}`}
                    p="lg"
                    h="100%"
                    style={{ textDecoration: 'none', color: 'inherit' }}
                  >
                    <Card.Section mb="md">
                      <CoverPhoto
                        src={COVER_PHOTOS[guide.code].card}
                        alt={COVER_PHOTOS[guide.code].alt}
                        height={150}
                        position={COVER_PHOTOS[guide.code].position}
                      />
                    </Card.Section>
                    <Stack gap="sm" h="100%">
                      <ThemeIcon size={40} radius="md" variant="light" color={style.color}>
                        <style.icon size={22} stroke={1.6} />
                      </ThemeIcon>
                      <Title order={3} fz="lg">
                        {guide.name}
                      </Title>
                      <Text size="sm" c="dimmed" style={{ flex: 1 }}>
                        {guide.tagline}
                      </Text>
                      <Group gap={4} c={style.color} fw={600} fz="sm">
                        View plan details <IconArrowRight size={16} />
                      </Group>
                    </Stack>
                  </Card>
                </Reveal>
              )
            })}
          </SimpleGrid>

          <Stack gap="md">
            <Title order={2} fz={{ base: 24, sm: 30 }}>
              Side by side
            </Title>
            <Card padding={0} style={{ overflow: 'hidden' }}>
              <Table.ScrollContainer minWidth={720}>
                <Table highlightOnHover>
                  <Table.Thead bg="light-dark(var(--mantine-color-brand-0), var(--mantine-color-dark-6))">
                    <Table.Tr>
                      <Table.Th>Cover</Table.Th>
                      <Table.Th>Protects</Table.Th>
                      <Table.Th>Premium basis</Table.Th>
                      <Table.Th>Best for</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {COVER_GUIDES.map((guide) => (
                      <Table.Tr key={guide.code}>
                        <Table.Td>
                          <Anchor component={Link} to={`/insurance/${guide.slug}`} fw={600}>
                            {guide.name}
                          </Anchor>
                        </Table.Td>
                        <Table.Td>{guide.protects}</Table.Td>
                        <Table.Td>{guide.premiumBasis}</Table.Td>
                        <Table.Td>{guide.bestFor}</Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            </Card>
            <Text size="xs" c="dimmed">
              Premiums rise by 15% for customers over 60. Cover is offered from age 18 to 75. Figures are indicative.
            </Text>
          </Stack>

          <Stack gap="md">
            <Title order={2} fz={{ base: 24, sm: 30 }}>
              Which cover is right for me?
            </Title>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
              {GUIDE_QUESTIONS.map((item) => (
                <Card key={item.slug} className="lift-soft" p="md">
                  <Group wrap="nowrap" justify="space-between">
                    <Group wrap="nowrap">
                      <ThemeIcon radius="xl" size={30} color="teal" variant="light">
                        <IconCheck size={16} stroke={3} />
                      </ThemeIcon>
                      <Text fw={500}>{item.question}</Text>
                    </Group>
                    <Anchor
                      component={Link}
                      to={`/insurance/${item.slug}`}
                      fw={600}
                      size="sm"
                      style={{ whiteSpace: 'nowrap' }}
                    >
                      {item.answer}
                    </Anchor>
                  </Group>
                </Card>
              ))}
            </SimpleGrid>
          </Stack>
        </Stack>
      </Container>
    </>
  )
}
