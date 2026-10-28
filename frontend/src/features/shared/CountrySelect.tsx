import { Select, type SelectProps } from '@mantine/core'
import * as flags from 'country-flag-icons/react/3x2'

import { COUNTRIES, findCountry } from './countries'

export function Flag({ code, width = 20 }: { code: string; width?: number }) {
  const Icon = (
    flags as unknown as Record<string, React.ComponentType<{ title?: string; style?: React.CSSProperties }>>
  )[code.toUpperCase()]
  return Icon ? <Icon style={{ width, borderRadius: 2, display: 'block' }} /> : null
}

const data = COUNTRIES.map((country) => ({ value: country.code, label: country.name }))

interface Props extends Omit<SelectProps, 'data' | 'onChange' | 'value'> {
  value: string
  onChange(code: string): void
}

export function CountrySelect({ value, onChange, ...rest }: Props) {
  const selected = value ? findCountry(value) : undefined
  return (
    <Select
      searchable
      clearable
      nothingFoundMessage="No country found"
      placeholder="Choose a country"
      data={data}
      value={selected?.code ?? null}
      onChange={(code) => onChange(code ?? '')}
      leftSection={selected ? <Flag code={selected.code} /> : undefined}
      renderOption={({ option }) => (
        <span style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Flag code={option.value} />
          {option.label}
        </span>
      )}
      {...rest}
    />
  )
}
