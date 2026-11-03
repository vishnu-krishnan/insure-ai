import {
  Alert,
  Badge,
  Box,
  Button,
  Card,
  Group,
  Loader,
  Pagination,
  SimpleGrid,
  Stack,
  Text,
  UnstyledButton,
} from '@mantine/core'
import { IconArrowRight, IconFilePlus } from '@tabler/icons-react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'

import { listMyQuotes } from '../../api/me'
import { COVER_GUIDES } from '../insurance/catalogue'
import { COVER_PHOTOS } from '../insurance/photos'
import { formatMoney } from '../quotes/format'
import { coverageStyle } from '../shared/coverage'
import { describeError } from '../shared/errors'
import { MeHeader } from './MeHeader'

const PAGE_SIZE = 9

export function MyQuotesPage() {
  const [page, setPage] = useState(0)
  const quotes = useQuery({
    queryKey: ['my-quotes', page],
    queryFn: () => listMyQuotes(page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  })

  return (
    <Stack gap="xl">
      <MeHeader
        eyebrow="My account"
        title="My quotes"
        description="Every quote you have requested, newest first. Open one to see the full premium breakdown."
        actions={
          <Button component={Link} to="/me/quotes/new" size="md" leftSection={<IconFilePlus size={18} />}>
            Get a quote
          </Button>
        }
      />
      {quotes.isPending && <Loader aria-label="Loading your quotes" />}
      {quotes.isError && <Alert color="red" title={describeError(quotes.error).message} />}
      {quotes.data && quotes.data.content.length === 0 && (
        <Card p={{ base: 'lg', sm: 48 }} radius="xl">
          <Stack align="center" gap="md" ta="center">
            <Text fw={800} fz={22}>
              No quotes yet
            </Text>
            <Text c="dimmed" maw={460}>
              Pick a type of cover to see your premium in seconds. Your quotes will be kept here.
            </Text>
            <Group justify="center" gap="xs">
              {COVER_GUIDES.map((guide) => (
                <Button
                  key={guide.code}
                  component={Link}
                  to={`/me/quotes/new?type=${guide.code}`}
                  variant="light"
                  radius="xl"
                >
                  {guide.name}
                </Button>
              ))}
            </Group>
          </Stack>
        </Card>
      )}
      {quotes.data && quotes.data.content.length > 0 && (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="lg">
          {quotes.data.content.map((quote) => {
            const style = coverageStyle(quote.coverageType)
            const photo = COVER_PHOTOS[quote.coverageType]
            return (
              <UnstyledButton key={quote.quoteId} component={Link} to={`/me/quotes/${quote.quoteId}`}>
                <Card p={0} className="lift-soft" h="100%" style={{ overflow: 'hidden' }}>
                  <Box h={110} pos="relative" style={{ overflow: 'hidden' }}>
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
                    <Box
                      pos="absolute"
                      inset={0}
                      style={{ background: 'linear-gradient(180deg, rgba(4,12,38,0) 30%, rgba(4,12,38,0.7))' }}
                    />
                    <Group pos="absolute" left={14} right={14} bottom={10} justify="space-between">
                      <Text c="white" fw={700}>
                        {style.label} cover
                      </Text>
                      {quote.status && (
                        <Badge variant="filled" color={quote.status === 'EXPIRED' ? 'gray' : 'brand'}>
                          {quote.status === 'EXPIRED' ? 'Expired' : 'Issued'}
                        </Badge>
                      )}
                    </Group>
                  </Box>
                  <Stack gap="sm" p="lg">
                    <Group justify="space-between" align="flex-end">
                      <Stack gap={0}>
                        <Text size="xs" c="dimmed" fw={650} tt="uppercase" style={{ letterSpacing: 0.6 }}>
                          {quote.quoteNumber}
                        </Text>
                        <Text fw={800} fz={26} lh={1.2}>
                          {formatMoney(quote.annualPremium)}
                        </Text>
                        <Text size="xs" c="dimmed">
                          per year · {formatMoney(quote.monthlyPremium)} a month
                        </Text>
                      </Stack>
                    </Group>
                    <Group
                      justify="space-between"
                      pt="sm"
                      style={{ borderTop: '1px solid var(--mantine-color-default-border)' }}
                    >
                      <Text size="sm" c="dimmed">
                        Cover {formatMoney(quote.coverageAmount)}
                      </Text>
                      <Text size="sm" fw={650} c="brand" style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        View <IconArrowRight size={14} />
                      </Text>
                    </Group>
                  </Stack>
                </Card>
              </UnstyledButton>
            )
          })}
        </SimpleGrid>
      )}
      {quotes.data && quotes.data.totalPages > 1 && (
        <Group justify="center">
          <Pagination total={quotes.data.totalPages} value={page + 1} onChange={(value) => setPage(value - 1)} />
        </Group>
      )}
    </Stack>
  )
}
