import { Group, Input, NativeSelect } from '@mantine/core'
import { useState } from 'react'

const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
]
const pad = (value: number) => String(value).padStart(2, '0')

interface Props {
  /** ISO date (yyyy-mm-dd) or empty while any part is still unchosen. */
  value: string
  error?: string | undefined
  onChange(value: string): void
}

// Three dropdowns instead of a calendar: a birth date is years away, and a calendar opens on the wrong year.
export function DateOfBirthField({ value, error, onChange }: Props) {
  const [initialYear = '', initialMonth = '', initialDay = ''] = value.split('-')
  const [parts, setParts] = useState({ year: initialYear, month: initialMonth, day: initialDay })
  const thisYear = new Date().getFullYear()
  const years = Array.from({ length: 76 }, (_, index) => thisYear - 16 - index)

  function update(next: Partial<typeof parts>) {
    const merged = { ...parts, ...next }
    const daysInMonth =
      merged.year && merged.month ? new Date(Number(merged.year), Number(merged.month), 0).getDate() : 31
    if (merged.day && Number(merged.day) > daysInMonth) merged.day = pad(daysInMonth)
    setParts(merged)
    onChange(merged.year && merged.month && merged.day ? `${merged.year}-${merged.month}-${merged.day}` : '')
  }

  const days = Array.from(
    { length: parts.year && parts.month ? new Date(Number(parts.year), Number(parts.month), 0).getDate() : 31 },
    (_, index) => pad(index + 1),
  )

  return (
    <Input.Wrapper label="Date of birth" withAsterisk error={error}>
      <Group grow gap="xs" wrap="nowrap" mt={4}>
        <NativeSelect
          aria-label="Birth day"
          data={[{ value: '', label: 'Day' }, ...days]}
          value={parts.day}
          onChange={(event) => update({ day: event.currentTarget.value })}
        />
        <NativeSelect
          aria-label="Birth month"
          data={[
            { value: '', label: 'Month' },
            ...MONTHS.map((name, index) => ({ value: pad(index + 1), label: name })),
          ]}
          value={parts.month}
          onChange={(event) => update({ month: event.currentTarget.value })}
        />
        <NativeSelect
          aria-label="Birth year"
          data={[{ value: '', label: 'Year' }, ...years.map(String)]}
          value={parts.year}
          onChange={(event) => update({ year: event.currentTarget.value })}
        />
      </Group>
    </Input.Wrapper>
  )
}
