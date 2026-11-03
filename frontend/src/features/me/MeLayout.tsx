import { Avatar, Box, Group, Menu, NavLink, Text, ThemeIcon, UnstyledButton } from '@mantine/core'
import {
  IconChevronDown,
  IconFileText,
  IconHome,
  IconLayoutDashboard,
  IconLogout,
  IconShieldCheck,
  IconUser,
} from '@tabler/icons-react'
import { Link, NavLink as RouterLink, Outlet } from 'react-router-dom'

import { useAuth } from '../../auth/useAuth'

const LINKS = [
  { to: '/me', label: 'Overview', icon: IconLayoutDashboard, end: true },
  { to: '/me/profile', label: 'My profile', icon: IconUser, end: false },
  { to: '/me/quotes', label: 'My quotes', icon: IconFileText, end: false },
]

function initials(name: string): string {
  return (
    name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]!.toUpperCase())
      .join('') || '?'
  )
}

export function MeLayout() {
  const { user, signOut } = useAuth()
  const name = user?.name ?? 'Account'

  return (
    <Box
      className="me-shell"
      mih="100vh"
      bg="light-dark(#f4f7fc, var(--mantine-color-dark-8))"
      style={{ display: 'flex', flexDirection: 'column' }}
    >
      <Box
        component="header"
        px="md"
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 50,
          background: 'light-dark(white, var(--mantine-color-dark-7))',
          borderBottom: '1px solid var(--mantine-color-default-border)',
          boxShadow: '0 1px 12px rgba(10, 30, 80, 0.05)',
        }}
      >
        <Group h={64} justify="space-between" wrap="nowrap" maw={1160} mx="auto">
          <Group gap="xl" wrap="nowrap">
            <Link to="/" aria-label="Insure AI home" style={{ textDecoration: 'none', color: 'inherit' }}>
              <Group gap="sm" wrap="nowrap">
                <ThemeIcon
                  size={34}
                  radius="md"
                  variant="gradient"
                  gradient={{ from: 'brand.7', to: 'brand.4', deg: 135 }}
                >
                  <IconShieldCheck size={20} stroke={1.8} />
                </ThemeIcon>
                <Text fw={700} size="lg" lh={1}>
                  Insure AI
                </Text>
              </Group>
            </Link>
            <Box component="nav" aria-label="My account" visibleFrom="md">
              <Group gap={4} wrap="nowrap">
                {LINKS.map((link) => (
                  <NavLink
                    key={link.to}
                    component={RouterLink}
                    to={link.to}
                    end={link.end}
                    label={link.label}
                    leftSection={<link.icon size={17} stroke={1.7} />}
                    variant="light"
                    style={{
                      borderRadius: 'var(--mantine-radius-md)',
                      width: 'auto',
                      flex: '0 0 auto',
                      fontWeight: 550,
                    }}
                  />
                ))}
              </Group>
            </Box>
          </Group>
          <Menu position="bottom-end" width={220} shadow="md">
            <Menu.Target>
              <UnstyledButton aria-label="Account menu">
                <Group gap="xs" wrap="nowrap">
                  <Avatar color="brand" variant="filled" radius="xl" size={36}>
                    {initials(name)}
                  </Avatar>
                  <Text size="sm" fw={600} visibleFrom="sm" maw={160} truncate>
                    {name}
                  </Text>
                  <IconChevronDown size={14} />
                </Group>
              </UnstyledButton>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Item component={Link} to="/" leftSection={<IconHome size={16} />}>
                Website home
              </Menu.Item>
              <Menu.Item component={Link} to="/me/profile" leftSection={<IconUser size={16} />}>
                My profile
              </Menu.Item>
              <Menu.Divider />
              <Menu.Item color="red" leftSection={<IconLogout size={16} />} onClick={() => void signOut()}>
                Sign out
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        </Group>
        <Box
          component="nav"
          aria-label="My account sections"
          hiddenFrom="md"
          maw={1160}
          mx="auto"
          pb={8}
          style={{ overflowX: 'auto' }}
        >
          <Group gap={4} wrap="nowrap">
            {LINKS.map((link) => (
              <NavLink
                key={link.to}
                component={RouterLink}
                to={link.to}
                end={link.end}
                label={link.label}
                leftSection={<link.icon size={17} stroke={1.7} />}
                variant="light"
                style={{
                  borderRadius: 'var(--mantine-radius-md)',
                  width: 'auto',
                  flex: '0 0 auto',
                  whiteSpace: 'nowrap',
                }}
              />
            ))}
          </Group>
        </Box>
      </Box>
      <Box component="main" maw={1160} w="100%" mx="auto" px="md" py={{ base: 'lg', sm: 32 }} style={{ flex: 1 }}>
        <Outlet />
      </Box>
      <Box component="footer" py="md" px="md" ta="center">
        <Text size="xs" c="dimmed">
          © {new Date().getFullYear()} Insure AI · Figures shown are illustrations until a quote is issued.
        </Text>
      </Box>
    </Box>
  )
}
