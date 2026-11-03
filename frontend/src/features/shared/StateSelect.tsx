import { Select, TextInput, type TextInputProps } from '@mantine/core'
import { forwardRef } from 'react'

import { statesFor } from './states'

interface Props extends Omit<TextInputProps, 'value' | 'onChange'> {
  country: string
  value: string
  onChange(state: string): void
}

// A searchable list when we know the country's states, a plain text box otherwise.
export const StateSelect = forwardRef<HTMLInputElement, Props>(function StateSelect(
  { country, value, onChange, label, error, withAsterisk, ...rest },
  ref,
) {
  const states = country ? statesFor(country) : undefined
  if (!states) {
    return (
      <TextInput
        ref={ref}
        label={label}
        withAsterisk={withAsterisk}
        maxLength={80}
        error={error}
        placeholder={country ? undefined : 'Choose a country first'}
        disabled={!country}
        value={value}
        onChange={(event) => onChange(event.currentTarget.value)}
        {...rest}
      />
    )
  }
  return (
    <Select
      searchable
      clearable
      label={label}
      withAsterisk={withAsterisk}
      error={error}
      placeholder="Search or choose a state"
      nothingFoundMessage="No state found"
      data={states}
      value={states.includes(value) ? value : null}
      onChange={(next) => onChange(next ?? '')}
    />
  )
})
