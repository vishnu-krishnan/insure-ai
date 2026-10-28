import {
  ActionIcon,
  AppShell,
  Avatar,
  Box,
  Group,
  Menu,
  NavLink as MantineNavLink,
  Stack,
  Text,
  ThemeIcon,
  UnstyledButton,
  useComputedColorScheme,
  useMantineColorScheme,
} from '@mantine/core'
import {
  IconCalculator,
  IconFileText,
  IconLayoutDashboard,
  IconLogout,
  IconMoon,
  IconShieldCheck,
  IconSun,
  IconUsers,
} from '@tabler/icons-react'
import { useEffect } from 'react'
import { NavLink, Outlet } from 'react-router-dom'

import { COVER_GUIDES } from '../features/insurance/catalogue'
import { coverageStyle } from '../features/shared/coverage'
import { useAuth } from '../auth/useAuth'
import { publishColorScheme } from './themeSync'

const links = [
  { to: '/agent', label: 'Dashboard', end: true, icon: IconLayoutDashboard },
  { to: '/agent/customers', label: 'Customers', end: false, icon: IconUsers },
  { to: '/agent/quotes', label: 'All quotes', end: false, icon: IconFileText },
  { to: '/agent/calculator', label: 'Calculator', end: false, icon: IconCalculator },
]

function initials(name?: string): string {
  return (name ?? '?').slice(0, 2).toUpperCase()
}

function BottomNav() {
  return (
    <Group grow gap={0} h="100%" wrap="nowrap" aria-label="Main navigation" component="nav">
      {links.map((link) => (
        <UnstyledButton
          key={link.to}
          component={NavLink}
          to={link.to}
          end={link.end}
          h="100%"
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        >
          {({ isActive }: { isActive: boolean }) => (
            <Stack gap={2} align="center" c={isActive ? 'brand' : 'dimmed'}>
              <link.icon size={22} stroke={isActive ? 2.2 : 1.7} />
              <Text size="xs" fw={isActive ? 650 : 500}>
                {link.label}
              </Text>
            </Stack>
          )}
        </UnstyledButton>
      ))}
    </Group>
  )
}

export function Shell() {
  const { user, signOut } = useAuth()
  const { setColorScheme } = useMantineColorScheme()
  const scheme = useComputedColorScheme('light')
  const next = scheme === 'dark' ? 'light' : 'dark'

  useEffect(() => publishColorScheme(scheme), [scheme])

  return (
    <AppShell
      header={{ height: 60 }}
      footer={{ height: { base: 64, sm: 0 } }}
      navbar={{ width: 248, breakpoint: 'sm', collapsed: { mobile: true } }}
      padding={{ base: 'md', sm: 'xl' }}
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <ThemeIcon size={34} radius="md" variant="gradient" gradient={{ from: 'brand.7', to: 'brand.4', deg: 135 }}>
              <IconShieldCheck size={20} stroke={1.8} />
            </ThemeIcon>
            <Text fw={700} size="lg" lh={1}>
              Insure AI
            </Text>
          </Group>
          <Group gap="xs" wrap="nowrap">
            <ActionIcon
              variant="subtle"
              color="gray"
              size="lg"
              onClick={() => setColorScheme(next)}
              aria-label={next === 'dark' ? 'Dark theme' : 'Light theme'}
            >
              {next === 'dark' ? <IconMoon size={18} /> : <IconSun size={18} />}
            </ActionIcon>
            <Text size="sm" fw={500} visibleFrom="sm">
              {user?.name}
            </Text>
            <Menu position="bottom-end" width={200} shadow="md">
              <Menu.Target>
                <ActionIcon variant="transparent" size="lg" radius="xl" aria-label="Account menu">
                  <Avatar color="brand" radius="xl" size={34}>
                    {initials(user?.name)}
                  </Avatar>
                </ActionIcon>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Label>Signed in as {user?.name}</Menu.Label>
                <Menu.Item leftSection={<IconLogout size={16} />} onClick={() => void signOut()}>
                  Sign out
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="md">
        <Text size="xs" fw={650} c="dimmed" tt="uppercase" mb="xs" px="sm">
          Agent workspace
        </Text>
        {links.slice(0, 2).map((link) => (
          <MantineNavLink
            key={link.to}
            component={NavLink}
            to={link.to}
            end={link.end}
            label={link.label}
            leftSection={<link.icon size={18} stroke={1.7} />}
            variant="light"
            fw={500}
            style={{ borderRadius: 'var(--mantine-radius-md)', marginBottom: 4 }}
          />
        ))}
        <Text size="xs" fw={650} c="dimmed" tt="uppercase" mt="md" mb="xs" px="sm">
          Insurance
        </Text>
        {COVER_GUIDES.map((guide) => {
          const style = coverageStyle(guide.code)
          return (
            <MantineNavLink
              key={guide.slug}
              component={NavLink}
              to={`/agent/${guide.slug}`}
              label={`${style.label} insurance`}
              leftSection={<style.icon size={18} stroke={1.7} />}
              color={style.color}
              variant="light"
              fw={500}
              style={{ borderRadius: 'var(--mantine-radius-md)', marginBottom: 4 }}
            />
          )
        })}
        <Text size="xs" fw={650} c="dimmed" tt="uppercase" mt="md" mb="xs" px="sm">
          Tools
        </Text>
        {links.slice(2).map((link) => (
          <MantineNavLink
            key={link.to}
            component={NavLink}
            to={link.to}
            end={link.end}
            label={link.label}
            leftSection={<link.icon size={18} stroke={1.7} />}
            variant="light"
            fw={500}
            style={{ borderRadius: 'var(--mantine-radius-md)', marginBottom: 4 }}
          />
        ))}
      </AppShell.Navbar>
      <AppShell.Footer hiddenFrom="sm">
        <BottomNav />
      </AppShell.Footer>
      <AppShell.Main bg="light-dark(#f5f7fb, var(--mantine-color-dark-8))">
        <Box maw={1200} mx="auto">
          <Outlet />
        </Box>
      </AppShell.Main>
    </AppShell>
  )
}
