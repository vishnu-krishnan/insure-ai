import {
  Accordion,
  Badge,
  Box,
  Button,
  Card,
  Container,
  Group,
  List,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconArrowRight,
  IconCheck,
  IconFileSearch,
  IconId,
  IconListSearch,
  IconReceipt2,
  IconUserPlus,
  IconUsers,
} from '@tabler/icons-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

import { useAuth } from '../../auth/useAuth'
import { Reveal } from '../Reveal'
import { AutoArt, HealthArt, HomeArt, LifeArt } from './Art'
import { CoverPhoto } from '../../features/shared/CoverPhoto'
import { PhotoSlot } from './PhotoSlot'

const covers: {
  title: string
  text: string
  points: string[]
  art: ReactNode
  tint: string
  photo: string
  slug: string
}[] = [
  {
    title: 'Auto insurance',
    photo: '/images/cover-auto.jpg',
    slug: 'auto',
    text: 'Cover for the car and the people in it.',
    points: ['Quote from the cover amount you choose', 'Monthly and annual premium shown together'],
    art: <AutoArt />,
    tint: 'var(--mantine-color-brand-light)',
  },
  {
    title: 'Home insurance',
    photo: '/images/cover-home.jpg',
    slug: 'home',
    text: 'Protection for the home and what is inside it.',
    points: ['Cover amount you choose', 'Premium priced from the cover and age'],
    art: <HomeArt />,
    tint: 'var(--mantine-color-teal-light)',
  },
  {
    title: 'Life insurance',
    photo: '/images/cover-life.jpg',
    slug: 'life',
    text: 'A financial safety net for the family.',
    points: ['Cover for ages 18 to 75', 'Quotes kept against the customer'],
    art: <LifeArt />,
    tint: 'var(--mantine-color-violet-light)',
  },
  {
    title: 'Health insurance',
    photo: '/images/cover-health.jpg',
    slug: 'health',
    text: 'Support when medical costs arrive.',
    points: ['Quote in a single step', 'Find any quote by its ID'],
    art: <HealthArt />,
    tint: 'var(--mantine-color-red-light)',
  },
]

const coverAccents = ['brand', 'brand', 'brand', 'brand']

const strengths = [
  {
    icon: IconUserPlus,
    title: 'Customer profiles',
    text: 'Register a customer once, with contact details, address and nominee.',
  },
  {
    icon: IconReceipt2,
    title: 'Clear premiums',
    text: 'Annual and monthly premium, tax and total, laid out side by side.',
  },
  {
    icon: IconListSearch,
    title: 'Find anything fast',
    text: 'Search customers and quotes by name, ID, email, phone or cover.',
  },
  {
    icon: IconId,
    title: 'Readable IDs',
    text: 'Every customer and quote has an ID such as CUS-000001 or QT-000001.',
  },
]

const steps = [
  {
    icon: IconUsers,
    title: 'Register the customer',
    text: 'Add name, contact details and date of birth.',
  },
  {
    icon: IconFileSearch,
    title: 'Choose the cover',
    text: 'Pick Auto, Home, Life or Health and the cover amount.',
  },
  {
    icon: IconCheck,
    title: 'Share the quote',
    text: 'The premium is calculated and saved with its own ID.',
  },
]

const faqs = [
  {
    q: 'How do I get a quote?',
    a: 'Sign in as an agent, register the customer if they are new, then create a quote for them. The premium is worked out straight away and the quote is saved with its own ID.',
  },
  {
    q: 'Which types of cover can be quoted?',
    a: 'Auto, Home, Life and Health. More cover types and product details will be added as the platform grows.',
  },
  {
    q: 'Can I try the numbers before signing in?',
    a: 'Yes. The calculator needs no sign-in and works out simple and compound interest, loan instalments and recurring deposits. Its figures are for illustration only.',
  },
  {
    q: 'How do I find an existing customer or quote?',
    a: 'Use the search on the Customers or Quotes screen. It matches names, IDs, email addresses, phone numbers and cover types.',
  },
]

function Section({ id, children, tint = false }: { id?: string; children: ReactNode; tint?: boolean }) {
  return (
    <Box
      id={id}
      py={{ base: 48, sm: 80 }}
      bg={tint ? 'light-dark(rgba(232, 239, 252, 0.72), var(--mantine-color-dark-7))' : undefined}
    >
      <Container size={1160}>{children}</Container>
    </Box>
  )
}

function Heading({ eyebrow, title, text }: { eyebrow: string; title: string; text?: string }) {
  return (
    <Stack gap="xs" align="center" ta="center" mb={{ base: 28, sm: 44 }}>
      <Badge variant="light" size="lg">
        {eyebrow}
      </Badge>
      <Title order={2} fz={{ base: 26, sm: 36 }} maw={680}>
        {title}
      </Title>
      <Box className="heading-bar" />
      {text && (
        <Text c="dimmed" maw={620} fz={{ base: 'sm', sm: 'md' }}>
          {text}
        </Text>
      )}
    </Stack>
  )
}

export function LandingPage() {
  const { signIn } = useAuth()

  return (
    <>
      <Box
        style={{
          color: 'white',
          position: 'relative',
          background: 'var(--insure-navy-gradient)',
          overflow: 'hidden',
        }}
      >
        <Box visibleFrom="md" className="hero-photo-bg" />
        <Container size={1160} py={{ base: 64, sm: 128 }} style={{ position: 'relative', zIndex: 1 }}>
          <Group justify="space-between" wrap="nowrap" gap={48}>
            <Stack gap="xl" maw={600}>
              <Stack gap="md">
                <Title order={1} c="white" fz={{ base: 36, sm: 58 }} lh={1.05} className="hero-title">
                  Insurance that stays <span className="hero-accent">clear and simple</span>
                </Title>
                <Text c="white" opacity={0.88} fz={{ base: 'md', sm: 20 }} lh={1.55} maw={540}>
                  Register customers, price cover and find any quote in seconds. Everything is kept together, with IDs
                  you can read out over the phone.
                </Text>
              </Stack>
              <Group>
                <Button
                  size="xl"
                  className="btn-on-dark"
                  component={Link}
                  to="/calculator"
                  rightSection={<IconArrowRight size={20} />}
                >
                  Estimate your premium
                </Button>
              </Group>
              <SimpleGrid cols={3} spacing="lg" className="hero-stats">
                {[
                  ['4', 'cover types'],
                  ['1 step', 'to a quote'],
                  ['18–75', 'customer ages'],
                ].map(([value, label]) => (
                  <Stack key={label} gap={2} className="hero-stat">
                    <Text className="hero-stat-value">{value}</Text>
                    <Text className="hero-stat-label">{label}</Text>
                  </Stack>
                ))}
              </SimpleGrid>
            </Stack>
          </Group>
        </Container>
      </Box>

      <Box className="line-section">
        <Section id="cover">
          <Heading
            eyebrow="Cover"
            title="Insurance for the things that matter most"
            text="Four types of cover, priced from the amount you choose and the customer's age."
          />
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }} spacing="lg">
            {covers.map((cover, index) => (
              <Reveal key={cover.title} delay={index * 90}>
                <Card
                  className="lift"
                  component={Link}
                  to={`/insurance/${cover.slug}`}
                  p="lg"
                  h="100%"
                  style={{
                    textDecoration: 'none',
                    color: 'inherit',
                    borderTop: `4px solid var(--mantine-color-${coverAccents[index]}-6)`,
                  }}
                >
                  <Stack gap="md" h="100%">
                    <PhotoSlot
                      src={cover.photo}
                      alt={cover.title}
                      height={150}
                      fallback={
                        <Box
                          h="100%"
                          style={{
                            background: cover.tint,
                            display: 'grid',
                            placeItems: 'center',
                          }}
                        >
                          {cover.art}
                        </Box>
                      }
                    />
                    <Title order={3} fz="lg">
                      {cover.title}
                    </Title>
                    <Text size="sm" c="dimmed">
                      {cover.text}
                    </Text>
                    <List
                      size="sm"
                      spacing={4}
                      icon={
                        <ThemeIcon size={16} radius="xl" color="brand" variant="light">
                          <IconCheck size={11} stroke={3} />
                        </ThemeIcon>
                      }
                    >
                      {cover.points.map((point) => (
                        <List.Item key={point}>{point}</List.Item>
                      ))}
                    </List>
                    <Group gap={4} mt="auto" c="brand" fw={650} fz="sm">
                      Explore {cover.title.split(' ')[0]} cover <IconArrowRight size={16} />
                    </Group>
                  </Stack>
                </Card>
              </Reveal>
            ))}
          </SimpleGrid>
        </Section>

        <Section tint>
          <Heading eyebrow="Why Insure AI" title="Everything an agent needs, in one place" />
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }} spacing="lg">
            {strengths.map((item, index) => (
              <Reveal key={item.title} delay={index * 80}>
                <Card className="lift" p="lg" h="100%">
                  <Stack gap="sm">
                    <ThemeIcon size={46} radius="md" variant="light">
                      <item.icon size={24} stroke={1.6} />
                    </ThemeIcon>
                    <Text fw={650}>{item.title}</Text>
                    <Text size="sm" c="dimmed">
                      {item.text}
                    </Text>
                  </Stack>
                </Card>
              </Reveal>
            ))}
          </SimpleGrid>
        </Section>

        <Section id="how-it-works">
          <Heading eyebrow="How it works" title="From customer to quote in three steps" />
          <SimpleGrid cols={{ base: 1, md: 3 }} spacing="lg">
            {steps.map((step, index) => (
              <Reveal key={step.title} delay={index * 110}>
                <Card className="lift" p="xl" h="100%">
                  <Stack gap="sm">
                    <Group justify="space-between">
                      <ThemeIcon
                        size={52}
                        radius="lg"
                        variant="gradient"
                        gradient={{ from: 'brand.7', to: 'brand.4', deg: 135 }}
                      >
                        <step.icon size={26} stroke={1.6} />
                      </ThemeIcon>
                      <Text fz={52} fw={800} lh={1} className="step-num">
                        {index + 1}
                      </Text>
                    </Group>
                    <Text fw={650} fz="lg">
                      {step.title}
                    </Text>
                    <Text c="dimmed">{step.text}</Text>
                  </Stack>
                </Card>
              </Reveal>
            ))}
          </SimpleGrid>
          <Box mt="xl">
            <CoverPhoto
              src="/images/agent-handshake.jpg"
              alt="An agent greeting an older couple at a desk"
              height={320}
              position="center 40%"
              radius="var(--mantine-radius-xl)"
            />
          </Box>
        </Section>

        <Section>
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing={48} style={{ alignItems: 'center' }}>
            <CoverPhoto
              src="/images/customer-keys.jpg"
              alt="A couple holding the keys to their home"
              height={380}
              position="center 30%"
              radius="var(--mantine-radius-xl)"
            />
            <Stack gap="md">
              <Badge variant="light" size="lg" w="fit-content">
                At the desk
              </Badge>
              <Title order={2} fz={{ base: 26, sm: 34 }}>
                A clear answer while the customer is still in the room
              </Title>
              <Text c="dimmed">
                Everything about a customer sits on one screen, with their quotes grouped by type of cover.
              </Text>
              <List
                spacing={8}
                icon={
                  <ThemeIcon size={22} radius="xl" color="brand" variant="light">
                    <IconCheck size={13} stroke={3} />
                  </ThemeIcon>
                }
              >
                <List.Item>Monthly and annual premium shown together, with tax</List.Item>
                <List.Item>Every quote kept against the customer it was made for</List.Item>
                <List.Item>Find anyone by name, ID, email or phone</List.Item>
              </List>
            </Stack>
          </SimpleGrid>
        </Section>

        <Section id="faq" tint>
          <Heading eyebrow="FAQ" title="Quick answers to common questions" />
          <Box maw={760} mx="auto">
            <Accordion variant="separated" radius="md" defaultValue={null}>
              {faqs.map((item) => (
                <Accordion.Item key={item.q} value={item.q}>
                  <Accordion.Control>{item.q}</Accordion.Control>
                  <Accordion.Panel>
                    <Text c="dimmed">{item.a}</Text>
                  </Accordion.Panel>
                </Accordion.Item>
              ))}
            </Accordion>
          </Box>
        </Section>

        <Section>
          <Box
            style={{
              borderRadius: 'var(--mantine-radius-xl)',
              color: 'white',
              overflow: 'hidden',
              background: 'var(--insure-navy-gradient)',
            }}
          >
            <SimpleGrid cols={{ base: 1, md: 2 }} spacing={0}>
              <Stack gap="md" p={{ base: 'xl', sm: 56 }} justify="center">
                <Title order={2} c="white" fz={{ base: 26, sm: 36 }}>
                  Ready to create your first quote?
                </Title>
                <Text c="white" opacity={0.9} maw={520}>
                  Sign in to register a customer and price their cover.
                </Text>
                <Group>
                  <Button size="lg" className="btn-on-dark" onClick={() => void signIn('/')}>
                    Sign in to get started
                  </Button>
                </Group>
              </Stack>
              <Box visibleFrom="md" mih={300}>
                <CoverPhoto
                  src="/images/agent-team.jpg"
                  alt="An agent and two customers giving a thumbs up"
                  height="100%"
                />
              </Box>
            </SimpleGrid>
          </Box>
        </Section>
      </Box>
    </>
  )
}
