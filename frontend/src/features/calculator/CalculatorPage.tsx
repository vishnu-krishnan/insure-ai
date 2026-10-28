import {
  Card,
  Group,
  NativeSelect,
  ScrollArea,
  NumberInput,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Table,
  Tabs,
  Text,
} from '@mantine/core'
import { IconCalculator } from '@tabler/icons-react'
import { useState, type ReactNode } from 'react'

import { PageHeader } from '../shared/PageHeader'
import { formatMoney } from '../quotes/format'
import { compoundInterest, loanSchedule, recurringDeposit, simpleInterest } from './math'

type Field = number | string

function isNum(value: Field): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

function Figure({ label, value, strong = false }: { label: string; value: string; strong?: boolean }) {
  return (
    <Stack gap={2}>
      <Text size="sm" c="white" opacity={0.8}>
        {label}
      </Text>
      <Text fw={strong ? 700 : 600} fz={strong ? 30 : 20} c="white">
        {value}
      </Text>
    </Stack>
  )
}

function Panel({ inputs, result, details }: { inputs: ReactNode; result: ReactNode; details?: ReactNode }) {
  return (
    <Stack gap="lg">
      <SimpleGrid cols={{ base: 1, md: 2 }} spacing="lg">
        <Card p={{ base: 'md', sm: 'xl' }}>
          <Stack>{inputs}</Stack>
        </Card>
        <Card
          p={{ base: 'md', sm: 'xl' }}
          style={{ background: 'linear-gradient(135deg, var(--mantine-color-brand-9), var(--mantine-color-brand-6))' }}
        >
          {result}
        </Card>
      </SimpleGrid>
      {details && (
        <Card p={{ base: 'md', sm: 'xl' }}>
          <Stack gap="sm">
            <Text fw={650} fz="lg">
              Year by year
            </Text>
            <ScrollArea.Autosize mah={340} type="auto" offsetScrollbars>
              {details}
            </ScrollArea.Autosize>
          </Stack>
        </Card>
      )}
    </Stack>
  )
}

const NEED_VALUES = <Text c="white">Enter an amount, rate and period to see the result</Text>

function InterestTab() {
  const [kind, setKind] = useState('Simple')
  const [principal, setPrincipal] = useState<Field>(100000)
  const [rate, setRate] = useState<Field>(8)
  const [years, setYears] = useState<Field>(5)
  const [frequency, setFrequency] = useState('4')

  let result: ReactNode = NEED_VALUES
  let details: ReactNode = null
  if (isNum(principal) && isNum(rate) && isNum(years) && principal > 0 && rate >= 0 && years > 0) {
    const input = { principal, annualRate: rate, years }
    const simple = kind === 'Simple'
    const calculated = simple
      ? { ...simpleInterest(input), yearly: [] }
      : compoundInterest({ ...input, timesPerYear: Number(frequency) })
    result = (
      <Stack>
        <SimpleGrid cols={2}>
          <Figure label="Interest earned" value={formatMoney(calculated.interest)} />
          <Figure label="Maturity amount" value={formatMoney(calculated.maturity)} strong />
        </SimpleGrid>
        {!simple && (
          <>
            <Text size="sm" c="dimmed">
              Compounded {['', 'yearly', 'half-yearly', '', 'quarterly'][Number(frequency)] ?? 'monthly'}
            </Text>
          </>
        )}
      </Stack>
    )
    if (!simple) {
      details = (
        <Table.ScrollContainer minWidth={240} type="native">
          <Table verticalSpacing="xs" striped highlightOnHover stickyHeader>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Year</Table.Th>
                <Table.Th ta="right">Balance</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {calculated.yearly.map((row) => (
                <Table.Tr key={row.year}>
                  <Table.Td>{row.year}</Table.Td>
                  <Table.Td ta="right">{formatMoney(row.balance)}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )
    }
  }

  return (
    <Panel
      inputs={
        <>
          <SegmentedControl data={['Simple', 'Compound']} value={kind} onChange={setKind} fullWidth />
          <NumberInput label="Amount" value={principal} onChange={setPrincipal} min={0} thousandSeparator="," />
          <NumberInput
            label="Annual interest rate (%)"
            value={rate}
            onChange={setRate}
            min={0}
            max={100}
            decimalScale={2}
          />
          <NumberInput label="Period (years)" value={years} onChange={setYears} min={0} max={60} decimalScale={1} />
          {kind === 'Compound' && (
            <NativeSelect
              label="Compounding"
              value={frequency}
              onChange={(event) => setFrequency(event.currentTarget.value)}
              data={[
                { value: '1', label: 'Yearly' },
                { value: '2', label: 'Half-yearly' },
                { value: '4', label: 'Quarterly' },
                { value: '12', label: 'Monthly' },
              ]}
            />
          )}
        </>
      }
      result={result}
      details={details}
    />
  )
}

function LoanTab() {
  const [principal, setPrincipal] = useState<Field>(1000000)
  const [rate, setRate] = useState<Field>(9)
  const [years, setYears] = useState<Field>(20)

  let result: ReactNode = NEED_VALUES
  let details: ReactNode = null
  if (isNum(principal) && isNum(rate) && isNum(years) && principal > 0 && rate >= 0 && years > 0) {
    const loan = loanSchedule({ principal, annualRate: rate, months: Math.round(years * 12) })
    result = (
      <Stack>
        <Figure label="Monthly instalment" value={formatMoney(loan.emi)} strong />
        <SimpleGrid cols={2}>
          <Figure label="Total interest" value={formatMoney(loan.totalInterest)} />
          <Figure label="Total payable" value={formatMoney(loan.totalPayable)} />
        </SimpleGrid>
      </Stack>
    )
    details = (
      <Table.ScrollContainer minWidth={320} type="native">
        <Table verticalSpacing="xs" striped highlightOnHover stickyHeader>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Year</Table.Th>
              <Table.Th ta="right">Principal</Table.Th>
              <Table.Th ta="right">Interest</Table.Th>
              <Table.Th ta="right">Balance</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {loan.yearly.map((row) => (
              <Table.Tr key={row.year}>
                <Table.Td>{row.year}</Table.Td>
                <Table.Td ta="right">{formatMoney(row.principalPaid)}</Table.Td>
                <Table.Td ta="right">{formatMoney(row.interestPaid)}</Table.Td>
                <Table.Td ta="right">{formatMoney(Math.max(row.closingBalance, 0))}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    )
  }

  return (
    <Panel
      inputs={
        <>
          <NumberInput label="Loan amount" value={principal} onChange={setPrincipal} min={0} thousandSeparator="," />
          <NumberInput
            label="Annual interest rate (%)"
            value={rate}
            onChange={setRate}
            min={0}
            max={100}
            decimalScale={2}
          />
          <NumberInput label="Tenure (years)" value={years} onChange={setYears} min={0} max={40} decimalScale={1} />
        </>
      }
      result={result}
      details={details}
    />
  )
}

function DepositTab() {
  const [deposit, setDeposit] = useState<Field>(5000)
  const [rate, setRate] = useState<Field>(7)
  const [years, setYears] = useState<Field>(5)

  let result: ReactNode = NEED_VALUES
  if (isNum(deposit) && isNum(rate) && isNum(years) && deposit > 0 && rate >= 0 && years > 0) {
    const plan = recurringDeposit({ monthlyDeposit: deposit, annualRate: rate, months: Math.round(years * 12) })
    result = (
      <Stack>
        <Figure label="Maturity amount" value={formatMoney(plan.maturity)} strong />
        <SimpleGrid cols={2}>
          <Figure label="Total deposited" value={formatMoney(plan.deposited)} />
          <Figure label="Interest earned" value={formatMoney(plan.interest)} />
        </SimpleGrid>
      </Stack>
    )
  }

  return (
    <Panel
      inputs={
        <>
          <NumberInput label="Monthly deposit" value={deposit} onChange={setDeposit} min={0} thousandSeparator="," />
          <NumberInput
            label="Annual interest rate (%)"
            value={rate}
            onChange={setRate}
            min={0}
            max={100}
            decimalScale={2}
          />
          <NumberInput label="Period (years)" value={years} onChange={setYears} min={0} max={40} decimalScale={1} />
        </>
      }
      result={result}
    />
  )
}

export function CalculatorPage() {
  return (
    <Stack>
      <PageHeader
        icon={IconCalculator}
        eyebrow="Tools"
        title="Calculator"
        description="Work out interest, loan instalments and savings growth. No sign-in needed."
      />
      <Tabs defaultValue="interest" keepMounted={false} variant="pills">
        <Tabs.List mb="md">
          <Tabs.Tab value="interest">Interest</Tabs.Tab>
          <Tabs.Tab value="loan">Loan EMI</Tabs.Tab>
          <Tabs.Tab value="deposit">Recurring deposit</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="interest">
          <InterestTab />
        </Tabs.Panel>
        <Tabs.Panel value="loan">
          <LoanTab />
        </Tabs.Panel>
        <Tabs.Panel value="deposit">
          <DepositTab />
        </Tabs.Panel>
      </Tabs>
      <Group>
        <Text size="xs" c="dimmed">
          Figures are for illustration only. They are not an offer, and actual rates and charges may differ.
        </Text>
      </Group>
    </Stack>
  )
}
