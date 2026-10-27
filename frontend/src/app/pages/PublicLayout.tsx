import {
  ActionIcon,
  Anchor,
  Box,
  Button,
  Group,
  Menu,
  Text,
  ThemeIcon,
  useComputedColorScheme,
  useMantineColorScheme,
} from '@mantine/core'
import { IconMoon, IconShieldCheck, IconSun } from '@tabler/icons-react'
import { useEffect } from 'react'
import { Link, Outlet } from 'react-router-dom'

import { useAuth } from '../../auth/useAuth'
import { COVER_GUIDES } from '../../features/insurance/catalogue'
import { publishColorScheme } from '../themeSync'

export function PublicLayout({ wide = false }: { wide?: boolean }) {
  const { status, user, signIn } = useAuth()
  const { setColorScheme } = useMantineColorScheme()
  const scheme = useComputedColorScheme('light')
  const next = scheme === 'dark' ? 'light' : 'dark'

  useEffect(() => publishColorScheme(scheme), [scheme])

  return (
    <Box
      mih="100vh"
      bg="light-dark(#f5f7fb, var(--mantine-color-dark-8))"
      style={{ display: 'flex', flexDirection: 'column' }}
    >
      <Box
        component="header"
        h={64}
        px="md"
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 50,
          backdropFilter: 'blur(12px)',
          WebkitBackdropFilter: 'blur(12px)',
          background: 'light-dark(rgba(255,255,255,0.84), rgba(19,27,47,0.84))',
          borderBottom: '1px solid var(--mantine-color-default-border)',
        }}
      >
        <Group h="100%" justify="space-between" wrap="nowrap" maw={1200} mx="auto">
          <Group gap="sm" wrap="nowrap">
            <ThemeIcon size={34} radius="md" variant="gradient" gradient={{ from: 'brand.7', to: 'brand.4', deg: 135 }}>
              <IconShieldCheck size={20} stroke={1.8} />
            </ThemeIcon>
            <Text fw={700} size="lg" lh={1}>
              Insure AI
            </Text>
          </Group>
          <Group gap="xs" wrap="nowrap">
            <Menu trigger="hover" openDelay={80} closeDelay={150} position="bottom-start" width={240} shadow="md">
              <Menu.Target>
                <Anchor
                  component={Link}
                  to="/insurance"
                  className="nav-link"
                  size="sm"
                  fw={550}
                  c="dimmed"
                  visibleFrom="sm"
                  mr="sm"
                >
                  Insurance
                </Anchor>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item component={Link} to="/insurance">
                  Compare all plans
                </Menu.Item>
                <Menu.Divider />
                {COVER_GUIDES.map((guide) => (
                  <Menu.Item key={guide.code} component={Link} to={`/insurance/${guide.slug}`}>
                    {guide.name}
                  </Menu.Item>
                ))}
              </Menu.Dropdown>
            </Menu>
            <Anchor
              component={Link}
              to="/calculator"
              className="nav-link"
              size="sm"
              fw={550}
              c="dimmed"
              visibleFrom="sm"
              mr="sm"
            >
              Calculator
            </Anchor>
            <ActionIcon
              variant="subtle"
              color="gray"
              size="lg"
              onClick={() => setColorScheme(next)}
              aria-label={next === 'dark' ? 'Dark theme' : 'Light theme'}
            >
              {next === 'dark' ? <IconMoon size={18} /> : <IconSun size={18} />}
            </ActionIcon>
            {status !== 'authenticated' && (
              <Button size="sm" onClick={() => void signIn('/')}>
                Sign in
              </Button>
            )}
            {user && (
              <Button size="sm" variant="light" component="a" href="/">
                Open workspace
              </Button>
            )}
          </Group>
        </Group>
      </Box>
      <Box component="main" style={{ flex: 1 }}>
        {wide ? (
          <Outlet />
        ) : (
          <Box maw={1200} mx="auto" p={{ base: 'md', sm: 'xl' }}>
            <Outlet />
          </Box>
        )}
      </Box>
      <Box component="footer" py="xl" px="md" bg="#0b1222" c="white">
        <Group justify="space-between" maw={1160} mx="auto" gap="lg">
          <Group gap="sm">
            <ThemeIcon size={30} radius="md" variant="gradient" gradient={{ from: 'brand.7', to: 'brand.4', deg: 135 }}>
              <IconShieldCheck size={18} stroke={1.8} />
            </ThemeIcon>
            <Text fw={650}>Insure AI</Text>
          </Group>
          <Group gap="lg">
            <Anchor component={Link} to="/insurance" c="gray.4" size="sm">
              Insurance plans
            </Anchor>
            <Anchor component={Link} to="/calculator" c="gray.4" size="sm">
              Calculator
            </Anchor>
            <Anchor component={Link} to="/" c="gray.4" size="sm">
              Home
            </Anchor>
          </Group>
          <Text size="xs" c="gray.5">
            Figures shown are illustrations only and are not an offer of insurance.
          </Text>
        </Group>
      </Box>
    </Box>
  )
}
