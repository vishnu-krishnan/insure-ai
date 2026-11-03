import {
  Alert,
  Avatar,
  Badge,
  Box,
  Button,
  Card,
  Group,
  Loader,
  RingProgress,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
  UnstyledButton,
} from '@mantine/core'
import {
  IconArrowRight,
  IconCalculator,
  IconCheck,
  IconFilePlus,
  IconFileText,
  IconShieldCheck,
  IconStack2,
  IconUser,
} from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

import { getMyProfile, listMyQuotes } from '../../api/me'
import { useAuth } from '../../auth/useAuth'
import { COVER_GUIDES } from '../insurance/catalogue'
import { COVER_PHOTOS } from '../insurance/photos'
import { formatMoney } from '../quotes/format'
import { coverageStyle } from '../shared/coverage'
import { describeError } from '../shared/errors'
import { completeness, isProfileComplete } from './profileStatus'

function Stat({
  icon: Glyph,
  label,
  value,
  hint,
}: {
  icon: typeof IconFileText
  label: string
  value: string
  hint?: string
}) {
  return (
    <Card p="lg" className="lift-soft">
      <Group gap="md" wrap="nowrap">
        <ThemeIcon size={46} radius="md" variant="light" color="brand">
          <Glyph size={24} stroke={1.6} />
        </ThemeIcon>
        <Stack gap={0} style={{ minWidth: 0 }}>
          <Text size="xs" fw={650} tt="uppercase" c="dimmed" style={{ letterSpacing: 0.6 }}>
            {label}
          </Text>
          <Text fw={800} fz={24} lh={1.2} truncate>
            {value}
          </Text>
          {hint && (
            <Text size="xs" c="dimmed">
              {hint}
            </Text>
          )}
        </Stack>
      </Group>
    </Card>
  )
}

function Step({
  done,
  title,
  text,
  to,
  cta,
}: {
  done: boolean
  title: string
  text: string
  to?: string
  cta?: string
}) {
  return (
    <Group wrap="nowrap" align="flex-start" gap="md">
      <ThemeIcon size={30} radius="xl" variant={done ? 'filled' : 'light'} color="brand">
        {done ? <IconCheck size={17} /> : <IconArrowRight size={16} />}
      </ThemeIcon>
      <Stack gap={2} style={{ flex: 1 }}>
        <Text fw={650} td={done ? 'line-through' : undefined} c={done ? 'dimmed' : undefined}>
          {title}
        </Text>
        <Text size="sm" c="dimmed">
          {text}
        </Text>
      </Stack>
      {!done && to && (
        <Button component={Link} to={to} size="compact-md" variant="light">
          {cta}
        </Button>
      )}
    </Group>
  )
}

export function MeDashboard() {
  const { user } = useAuth()
  const profile = useQuery({ queryKey: ['my-profile'], queryFn: getMyProfile })
  const quotes = useQuery({
    queryKey: ['my-quotes', 'recent'],
    queryFn: () => listMyQuotes(0, 5),
    enabled: profile.data != null,
  })

  if (profile.isPending) {
    return <Loader aria-label="Loading your account" />
  }
  if (profile.isError) {
    return <Alert color="red" title={describeError(profile.error).message} />
  }

  const customer = profile.data
  const displayName = customer?.fullName ?? user?.name ?? ''
  const firstName = displayName.split(' ')[0]
  const percent = customer ? completeness(customer) : 0
  const recent = quotes.data?.content ?? []
  const hasQuotes = (quotes.data?.totalElements ?? 0) > 0
  const coverTypes = new Set(recent.map((quote) => quote.coverageType)).size
  const latest = recent[0]
  const ready = isProfileComplete(customer)
  const coverTarget = ready ? '/me/quotes/new' : '/me/profile'

  return (
    <Stack gap={32}>
      <Card
        p={{ base: 'lg', sm: 36 }}
        radius="xl"
        style={{
          background: 'var(--insure-navy-gradient)',
          color: 'white',
          border: 'none',
          backgroundImage: `url(/images/lines-light.svg), var(--insure-navy-gradient)`,
        }}
      >
        <Group justify="space-between" align="center" wrap="wrap" gap="xl">
          <Group gap="lg" wrap="nowrap" align="center">
            <Avatar size={64} radius="xl" color="white" variant="light" visibleFrom="xs" style={{ color: 'white' }}>
              <IconShieldCheck size={34} stroke={1.5} />
            </Avatar>
            <Stack gap={6}>
              <Title order={1} c="white" fz={{ base: 26, sm: 34 }} fw={800} style={{ letterSpacing: -0.5 }}>
                {firstName ? `Welcome back, ${firstName}` : 'Welcome'}
              </Title>
              <Text c="white" opacity={0.85} maw={520}>
                {ready
                  ? 'Review your details, track your quotes, or price new cover in a few steps.'
                  : 'Complete your profile once and you can request quotes straight away.'}
              </Text>
              <Group mt="sm" gap="sm">
                {ready ? (
                  <Button
                    className="btn-on-dark"
                    component={Link}
                    to="/me/quotes/new"
                    leftSection={<IconFilePlus size={18} />}
                  >
                    Get a quote
                  </Button>
                ) : (
                  <Button
                    className="btn-on-dark"
                    component={Link}
                    to="/me/profile"
                    leftSection={<IconUser size={18} />}
                  >
                    Complete your profile
                  </Button>
                )}
                <Button
                  variant="outline"
                  color="white"
                  component={Link}
                  to="/calculator"
                  leftSection={<IconCalculator size={18} />}
                >
                  Estimate your premium
                </Button>
              </Group>
            </Stack>
          </Group>
          <Group gap="md" wrap="nowrap">
            <RingProgress
              size={112}
              thickness={9}
              roundCaps
              sections={[{ value: percent, color: 'white' }]}
              rootColor="rgba(255,255,255,0.18)"
              label={
                <Text ta="center" fw={800} fz={22} c="white">
                  {percent}%
                </Text>
              }
            />
            <Stack gap={0}>
              <Text fw={650} c="white">
                Profile
              </Text>
              <Text size="sm" c="white" opacity={0.8}>
                {percent === 100 ? 'All set' : customer ? 'Finish every section to unlock quotes' : 'Not started yet'}
              </Text>
            </Stack>
          </Group>
        </Group>
      </Card>

      {customer && (
        <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="lg">
          <Stat
            icon={IconFileText}
            label="Quotes"
            value={String(quotes.data?.totalElements ?? 0)}
            hint="Requested so far"
          />
          <Stat
            icon={IconShieldCheck}
            label="Latest premium"
            value={latest ? `${formatMoney(latest.annualPremium)}` : '—'}
            hint={latest ? `${latest.quoteNumber} · per year` : 'No quote yet'}
          />
          <Stat icon={IconStack2} label="Cover types" value={String(coverTypes)} hint="Quoted so far" />
        </SimpleGrid>
      )}

      {!(customer && hasQuotes) && (
        <Card p={{ base: 'lg', sm: 'xl' }}>
          <Stack gap="lg">
            <Text fw={700} fz="lg">
              Get started
            </Text>
            <Step done title="Create your account" text="You are signed in and verified." />
            <Step
              done={ready}
              title="Complete your profile"
              text="Tell us a little about yourself once. We use it to price your cover."
              to="/me/profile"
              cta="Complete profile"
            />
            <Step
              done={hasQuotes}
              title="Request your first quote"
              text="Pick a cover type and see your premium straight away."
              to={coverTarget}
              cta="Get a quote"
            />
          </Stack>
        </Card>
      )}

      <Stack gap="md">
        <Group justify="space-between" align="flex-end">
          <Stack gap={2}>
            <Text fw={800} fz={22}>
              Choose your cover
            </Text>
            <Text c="dimmed" size="sm">
              Four ways to protect what matters. Pick one to start a quote.
            </Text>
          </Stack>
          <Button component={Link} to="/insurance" variant="subtle" rightSection={<IconArrowRight size={16} />}>
            Compare plans
          </Button>
        </Group>
        <SimpleGrid cols={{ base: 1, xs: 2, md: 4 }} spacing="lg">
          {COVER_GUIDES.map((guide) => {
            const photo = COVER_PHOTOS[guide.code]
            const Glyph = coverageStyle(guide.code).icon
            return (
              <UnstyledButton
                key={guide.code}
                component={Link}
                to={coverTarget === '/me/profile' ? coverTarget : `/me/quotes/new?type=${guide.code}`}
                aria-label={`Get ${guide.name} quote`}
              >
                <Card
                  p={0}
                  className="lift-soft"
                  h="100%"
                  style={{ overflow: 'hidden', borderTop: '3px solid var(--mantine-color-brand-6)' }}
                >
                  <Box h={130} style={{ overflow: 'hidden' }}>
                    {photo && (
                      <img
                        src={photo.card}
                        alt=""
                        loading="lazy"
                        style={{
                          width: '100%',
                          height: '100%',
                          objectFit: 'cover',
                          objectPosition: photo.position,
                          display: 'block',
                        }}
                      />
                    )}
                  </Box>
                  <Stack gap={6} p="md">
                    <Group gap="xs" wrap="nowrap">
                      <ThemeIcon size={28} radius="md" variant="light" color="brand">
                        <Glyph size={16} />
                      </ThemeIcon>
                      <Text fw={700}>{guide.name}</Text>
                    </Group>
                    <Text size="sm" c="dimmed" lineClamp={2}>
                      {guide.tagline}
                    </Text>
                    <Text size="sm" fw={650} c="brand">
                      Get a quote →
                    </Text>
                  </Stack>
                </Card>
              </UnstyledButton>
            )
          })}
        </SimpleGrid>
      </Stack>

      {customer && recent.length > 0 && (
        <Stack gap="md">
          <Group justify="space-between" align="flex-end">
            <Text fw={800} fz={22}>
              Recent quotes
            </Text>
            <Button component={Link} to="/me/quotes" variant="subtle" rightSection={<IconArrowRight size={16} />}>
              View all
            </Button>
          </Group>
          <Stack gap="sm">
            {recent.map((quote) => {
              const style = coverageStyle(quote.coverageType)
              const Glyph = style.icon
              return (
                <UnstyledButton key={quote.quoteId} component={Link} to={`/me/quotes/${quote.quoteId}`}>
                  <Card p="md" className="lift-soft">
                    <Group justify="space-between" wrap="nowrap">
                      <Group gap="md" wrap="nowrap">
                        <ThemeIcon size={42} radius="md" variant="light" color="brand">
                          <Glyph size={22} />
                        </ThemeIcon>
                        <Stack gap={0}>
                          <Text fw={700}>{quote.quoteNumber}</Text>
                          <Text size="sm" c="dimmed">
                            {style.label} cover · {formatMoney(quote.coverageAmount)}
                          </Text>
                        </Stack>
                      </Group>
                      <Group gap="md" wrap="nowrap">
                        {quote.status && (
                          <Badge variant="light" color={quote.status === 'EXPIRED' ? 'gray' : 'brand'} visibleFrom="xs">
                            {quote.status === 'EXPIRED' ? 'Expired' : 'Issued'}
                          </Badge>
                        )}
                        <Stack gap={0} align="flex-end">
                          <Text fw={800}>{formatMoney(quote.annualPremium)}</Text>
                          <Text size="xs" c="dimmed">
                            per year
                          </Text>
                        </Stack>
                      </Group>
                    </Group>
                  </Card>
                </UnstyledButton>
              )
            })}
          </Stack>
        </Stack>
      )}
    </Stack>
  )
}
