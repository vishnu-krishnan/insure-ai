import { NativeSelect, SimpleGrid, TextInput } from '@mantine/core'

import { OCCUPATIONS, OCCUPATION_SEPARATOR, splitOccupation } from '../../api/schemas'

interface Props {
  value: string
  error?: string | undefined
  onChange(value: string): void
}

// Asks what kind of work the customer does, then for details where that helps price the cover.
// Stored as one text value, "Category - details", because the customer record has a single occupation field.
export function OccupationField({ value, error, onChange }: Props) {
  const { category, details } = splitOccupation(value)
  const wanted = OCCUPATIONS.find((item) => item.value === category)?.detail ?? null
  const compose = (nextCategory: string, nextDetails: string) =>
    nextCategory === ''
      ? ''
      : nextDetails.trim()
        ? `${nextCategory}${OCCUPATION_SEPARATOR}${nextDetails}`
        : nextCategory

  return (
    <SimpleGrid cols={{ base: 1, sm: wanted ? 2 : 1 }}>
      <NativeSelect
        label="What do you do?"
        withAsterisk
        data={[{ value: '', label: 'Choose your occupation' }, ...OCCUPATIONS.map((item) => item.value)]}
        value={category}
        error={wanted ? undefined : error}
        onChange={(event) => onChange(compose(event.currentTarget.value, ''))}
      />
      {wanted && (
        <TextInput
          label="Occupation details"
          withAsterisk
          maxLength={60}
          placeholder={wanted}
          value={details}
          error={error}
          onChange={(event) => onChange(compose(category, event.currentTarget.value))}
        />
      )}
    </SimpleGrid>
  )
}
