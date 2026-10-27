import { Button, Card, NativeSelect, Table, TextInput, createTheme, type MantineColorsTuple } from '@mantine/core'

// Trust blue as the brand colour, with the standard Mantine teal, amber and red for money, warning and error.
const brand: MantineColorsTuple = [
  '#eef4ff',
  '#dbe7ff',
  '#b9cfff',
  '#8fb0ff',
  '#6690fb',
  '#3f6ff0',
  '#2557d6',
  '#1b45ae',
  '#153586',
  '#0f275f',
]

// Deep navy surfaces for dark mode instead of pure black.
const dark: MantineColorsTuple = [
  '#e8edf7',
  '#c5cfe3',
  '#9ba9c6',
  '#7685a8',
  '#4d5b7c',
  '#33405e',
  '#1d2740',
  '#131b2f',
  '#0b1222',
  '#070c18',
]

export const theme = createTheme({
  colors: { brand, dark },
  primaryColor: 'brand',
  primaryShade: { light: 6, dark: 4 },
  autoContrast: true,
  defaultRadius: 'md',
  fontFamily:
    '"Inter Variable", Inter, system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
  fontFamilyMonospace: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
  headings: { fontWeight: '650' },
  shadows: {
    xs: '0 1px 2px rgba(15, 39, 95, 0.06)',
    sm: '0 2px 8px rgba(15, 39, 95, 0.08)',
    md: '0 8px 24px rgba(15, 39, 95, 0.10)',
  },
  components: {
    Button: Button.extend({ defaultProps: { fw: 550 } }),
    Card: Card.extend({ defaultProps: { withBorder: true, shadow: 'xs', radius: 'lg' } }),
    TextInput: TextInput.extend({ defaultProps: { size: 'md' } }),
    NativeSelect: NativeSelect.extend({ defaultProps: { size: 'md' } }),
    Table: Table.extend({ defaultProps: { verticalSpacing: 'sm', horizontalSpacing: 'md' } }),
  },
})
