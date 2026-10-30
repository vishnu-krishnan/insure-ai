import {
  Box,
  Card,
  Group,
  List,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  ThemeIcon,
  Title,
  UnstyledButton,
} from '@mantine/core'
import {
  IconCalculator,
  IconFilePlus,
  IconFiles,
  IconUserPlus,
  IconUserSearch,
  IconUsers,
  IconFileText,
  type Icon,
} from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

import { searchCustomers } from '../../api/customers'
import { listQuotes } from '../../api/quotes'
import { useAuth } from '../../auth/useAuth'
import { COVER_GUIDES } from '../../features/insurance/catalogue'
import { portalPath } from '../../features/portal/paths'
import { coverageStyle } from '../../features/shared/coverage'
import { PageHeader } from '../../features/shared/PageHeader'
import { COVER_PHOTOS } from '../../features/insurance/photos'
import { CoverPhoto } from '../../features/shared/CoverPhoto'

interface Action {
  to: string
  title: string
  text: string
  icon: Icon
  color: string
}

const customerActions: Action[] = [
  {
    to: '/agent/customers/new',
    title: 'New customer',
    text: 'Register a customer profile.',
    icon: IconUserPlus,
    color: 'brand',
  },
  {
    to: '/agent/customers',
    title: 'Find a customer',
    text: 'Search by name, customer ID, email or phone.',
    icon: IconUserSearch,
    color: 'teal',
  },
]

const quoteActions: Action[] = [
  {
    to: '/agent/quotes/new',
    title: 'New quote',
    text: 'Price cover for an existing customer.',
    icon: IconFilePlus,
    color: 'violet',
  },
  {
    to: '/agent/quotes',
    title: 'All quotes',
    text: 'Browse and search quotes, newest first.',
    icon: IconFiles,
    color: 'orange',
  },
]

function ActionTile({ action }: { action: Action }) {
  return (
    <UnstyledButton
      component={Link}
      to={action.to}
      p="md"
      aria-label={action.title}
      style={{ border: '1px solid var(--mantine-color-default-border)', borderRadius: 'var(--mantine-radius-md)' }}
    >
      <Group wrap="nowrap" align="flex-start">
        <ThemeIcon size={40} radius="md" variant="light" color={action.color}>
          <action.icon size={22} stroke={1.7} />
        </ThemeIcon>
        <Stack gap={2}>
          <Text fw={600}>{action.title}</Text>
          <Text size="sm" c="dimmed">
            {action.text}
          </Text>
        </Stack>
      </Group>
    </UnstyledButton>
  )
}

interface ServiceProps {
  title: string
  description: string
  icon: Icon
  color: string
  stat: string | null
  loading: boolean
  capabilities: string[]
  actions: Action[]
  photo: string
  photoAlt: string
}

function ServiceCard({
  title,
  description,
  icon: Glyph,
  color,
  stat,
  loading,
  capabilities,
  actions,
  photo,
  photoAlt,
}: ServiceProps) {
  return (
    <Card p={{ base: 'md', sm: 'xl' }}>
      <Card.Section mb="md">
        <CoverPhoto src={photo} alt={photoAlt} height={130} position="center 35%" />
      </Card.Section>
      <Stack gap="md">
        <Group justify="space-between" align="flex-start" wrap="nowrap">
          <Group wrap="nowrap" align="flex-start">
            <ThemeIcon size={48} radius="lg" variant="light" color={color}>
              <Glyph size={26} stroke={1.6} />
            </ThemeIcon>
            <Stack gap={2}>
              <Title order={3} fz="lg">
                {title}
              </Title>
              <Text size="sm" c="dimmed">
                {description}
              </Text>
            </Stack>
          </Group>
        </Group>
        {loading ? (
          <Skeleton h={22} w={180} />
        ) : (
          stat && (
            <Text fw={650} c={color} fz="lg">
              {stat}
            </Text>
          )
        )}
        <List size="sm" spacing={4} c="dimmed">
          {capabilities.map((item) => (
            <List.Item key={item}>{item}</List.Item>
          ))}
        </List>
        <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
          {actions.map((action) => (
            <ActionTile key={action.to} action={action} />
          ))}
        </SimpleGrid>
      </Stack>
    </Card>
  )
}

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`
}

function PortalTile({ code }: { code: string }) {
  const style = coverageStyle(code)
  const count = useQuery({ queryKey: ['dashboard', 'portal', code], queryFn: () => listQuotes(0, 1, '', code) })
  return (
    <UnstyledButton component={Link} to={portalPath(code)} aria-label={`${style.label} insurance`}>
      <Card p="md" className="lift" h="100%">
        <Card.Section mb="md">
          <CoverPhoto src={COVER_PHOTOS[code].card} alt="" height={96} position={COVER_PHOTOS[code].position} />
        </Card.Section>
        <Group wrap="nowrap">
          <ThemeIcon size={44} radius="md" variant="light" color={style.color}>
            <style.icon size={24} stroke={1.6} />
          </ThemeIcon>
          <Stack gap={0}>
            <Text fw={650}>{`${style.label} insurance`}</Text>
            {count.data ? (
              <Text size="sm" c="dimmed">
                {plural(count.data.totalElements, 'quote', 'quotes')}
              </Text>
            ) : (
              <Skeleton h={14} w={70} mt={4} />
            )}
          </Stack>
        </Group>
      </Card>
    </UnstyledButton>
  )
}

export function Dashboard() {
  const { user } = useAuth()
  const customers = useQuery({ queryKey: ['dashboard', 'customers'], queryFn: () => searchCustomers('', 0, 1) })
  const quotes = useQuery({ queryKey: ['dashboard', 'quotes'], queryFn: () => listQuotes(0, 1) })

  return (
    <Stack gap="lg">
      <PageHeader
        title="Dashboard"
        description="Your day at a glance: open an insurance portal, register a customer or look up an existing quote."
      />
      <Box
        p={{ base: 'lg', sm: 'xl' }}
        style={{
          borderRadius: 'var(--mantine-radius-lg)',
          color: 'white',
          overflow: 'hidden',
          background: 'linear-gradient(135deg, var(--mantine-color-brand-9), var(--mantine-color-brand-6))',
        }}
      >
        <Group justify="space-between" wrap="nowrap">
          <Stack gap={4} style={{ minWidth: 0 }}>
            <Title order={3} c="white" fz={{ base: 'xl', sm: 28 }}>
              Welcome back, {user?.name}
            </Title>
            <Text c="white" opacity={0.9} maw={460}>
              Register a customer once, then quote any cover for them from its own portal. Every customer and quote has
              a short ID you can search by.
            </Text>
          </Stack>
          <Box visibleFrom="md" w={260} style={{ flexShrink: 0 }}>
            <CoverPhoto
              src="/images/agent-handshake.jpg"
              alt="An agent greeting a customer"
              height={150}
              radius="14px"
            />
          </Box>
        </Group>
      </Box>
      <Stack gap="xs">
        <Text fw={650} fz="lg">
          Insurance portals
        </Text>
        <SimpleGrid cols={{ base: 1, xs: 2, lg: 4 }} spacing="md">
          {COVER_GUIDES.map((guide) => (
            <PortalTile key={guide.code} code={guide.code} />
          ))}
        </SimpleGrid>
      </Stack>
      <SimpleGrid cols={{ base: 1, lg: 2 }} spacing="lg">
        <ServiceCard
          title="Customer service"
          description="Profiles, contact details, addresses and nominees for every policyholder."
          icon={IconUsers}
          color="brand"
          loading={customers.isPending}
          stat={
            customers.data ? plural(customers.data.totalElements, 'customer registered', 'customers registered') : null
          }
          capabilities={[
            'Register a customer with email, phone and date of birth',
            'Every customer gets an ID such as CUS-000001',
            'Search by name, ID, email or phone',
          ]}
          actions={customerActions}
          photo="/images/customer-keys.jpg"
          photoAlt="A couple holding the keys to their home"
        />
        <ServiceCard
          title="Quote service"
          description="Premium quotes for auto, home, life and health cover, kept against the customer they belong to."
          icon={IconFileText}
          color="violet"
          loading={quotes.isPending}
          stat={quotes.data ? plural(quotes.data.totalElements, 'quote issued', 'quotes issued') : null}
          capabilities={[
            'Price cover for a registered customer',
            'Every quote gets an ID such as QT-000001',
            'Monthly and annual premium shown together',
          ]}
          actions={quoteActions}
          photo="/images/customer-laptop.jpg"
          photoAlt="A couple going through paperwork at a laptop"
        />
      </SimpleGrid>
      <Card p={{ base: 'md', sm: 'lg' }}>
        <UnstyledButton component={Link} to="/agent/calculator" aria-label="Calculator">
          <Group wrap="nowrap">
            <ThemeIcon size={44} radius="lg" variant="light" color="teal">
              <IconCalculator size={24} stroke={1.6} />
            </ThemeIcon>
            <Stack gap={2}>
              <Text fw={600}>Calculator</Text>
              <Text size="sm" c="dimmed">
                Interest, loan instalments and recurring deposits, to talk through numbers with a customer.
              </Text>
            </Stack>
          </Group>
        </UnstyledButton>
      </Card>
    </Stack>
  )
}
