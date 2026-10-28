import { Group, Input, Select, TextInput } from '@mantine/core'
import { memo, useCallback, useRef, useState } from 'react'

import { Flag } from './CountrySelect'
import { COUNTRIES, DEFAULT_COUNTRY, joinPhone, splitPhone } from './countries'

const data = COUNTRIES.map((country) => ({
  value: country.code,
  label: `${country.name} (+${country.dial})`,
}))

interface Props {
  label: string
  value: string
  error?: string
  onChange(value: string): void
}

const DialSelect = memo(function DialSelect({
  label,
  country,
  onPick,
}: {
  label: string
  country: string
  onPick(code: string): void
}) {
  return (
    <Select
      aria-label={`${label} country`}
      searchable
      w={150}
      data={data}
      value={country}
      allowDeselect={false}
      leftSection={<Flag code={country} />}
      onChange={(code) => onPick(code ?? DEFAULT_COUNTRY)}
      renderOption={({ option }) => (
        <span style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Flag code={option.value} />
          {option.label}
        </span>
      )}
      comboboxProps={{ width: 300, position: 'bottom-start' }}
      styles={{ input: { textOverflow: 'clip' } }}
    />
  )
})

// Collects a country and a local number, and reports one international number such as +919876543210.
// Typing a full number that starts with "+" picks the country from it.
export function PhoneField({ label, value, error, onChange }: Props) {
  const initial = value ? splitPhone(value) : null
  const [country, setCountry] = useState<string>(initial?.country ?? DEFAULT_COUNTRY)
  const [typed, setTyped] = useState(initial?.national ?? '')
  const typedRef = useRef(typed)
  typedRef.current = typed
  const onChangeRef = useRef(onChange)
  onChangeRef.current = onChange

  function change(nextCountry: string, text: string) {
    if (text.trim().startsWith('+')) {
      onChange(text.replace(/[^\d+]/g, ''))
      const detected = splitPhone(text.replace(/[^\d+]/g, ''))
      if (detected) {
        setCountry(detected.country)
      }
      return
    }
    onChange(joinPhone(nextCountry, text))
  }

  const pick = useCallback((next: string) => {
    setCountry(next)
    const text = typedRef.current
    onChangeRef.current(text.trim().startsWith('+') ? text.replace(/[^\d+]/g, '') : joinPhone(next, text))
  }, [])

  return (
    <Input.Wrapper label={label} error={error}>
      <Group gap="xs" wrap="nowrap" align="flex-start" mt={4}>
        <DialSelect label={label} country={country} onPick={pick} />
        <TextInput
          aria-label={label}
          type="tel"
          autoComplete="tel-national"
          placeholder="9876543210"
          style={{ flex: 1 }}
          value={typed}
          error={!!error}
          onChange={(event) => {
            setTyped(event.currentTarget.value)
            change(country, event.currentTarget.value)
          }}
        />
      </Group>
    </Input.Wrapper>
  )
}
