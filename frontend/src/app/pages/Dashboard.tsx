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
import { PageHeader } from '../../features/shared/PageHeader'
import { HeroArt } from './HeroArt'

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
}

function ServiceCard({ title, description, icon: Glyph, color, stat, loading, capabilities, actions }: ServiceProps) {
  return (
    <Card p={{ base: 'md', sm: 'xl' }}>
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

export function Dashboard() {
  const { user } = useAuth()
  const customers = useQuery({ queryKey: ['dashboard', 'customers'], queryFn: () => searchCustomers('', 0, 1) })
  const quotes = useQuery({ queryKey: ['dashboard', 'quotes'], queryFn: () => listQuotes(0, 1) })

  return (
    <Stack gap="lg">
      <PageHeader title="Dashboard" description="Create customers and quotes, or find one you already have." />
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
              Start with a customer, then price a quote for them. Everything you create is searchable by its ID.
            </Text>
          </Stack>
          <Box visibleFrom="sm" style={{ flexShrink: 0 }}>
            <HeroArt size={190} />
          </Box>
        </Group>
      </Box>
      <SimpleGrid cols={{ base: 1, lg: 2 }} spacing="lg">
        <ServiceCard
          title="Customer service"
          description="Customer profiles and their contact details."
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
        />
        <ServiceCard
          title="Quote service"
          description="Premium quotes for AUTO, HOME, LIFE and HEALTH cover."
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
