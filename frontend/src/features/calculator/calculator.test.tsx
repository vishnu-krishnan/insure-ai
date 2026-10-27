import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderWithMantine } from '../../test/render'
import { CalculatorPage } from './CalculatorPage'

describe('calculator page', () => {
  it('shows simple interest on the default values', () => {
    renderWithMantine(<CalculatorPage />)

    expect(screen.getByText('Interest earned').nextSibling).toHaveTextContent('40,000.00')
    expect(screen.getByText('Maturity amount').nextSibling).toHaveTextContent('140,000.00')
  })

  it('switches to compound interest and lists the balance for each year', async () => {
    renderWithMantine(<CalculatorPage />)

    const user = userEvent.setup()
    await user.click(screen.getByRole('radio', { name: 'Compound' }))

    expect(await screen.findByText(/^Compounded/)).toBeInTheDocument()
    expect(screen.getAllByRole('row').length).toBeGreaterThan(5)
  })

  it('shows the monthly instalment for a loan', async () => {
    renderWithMantine(<CalculatorPage />)

    const user = userEvent.setup()
    await user.click(screen.getByRole('tab', { name: 'Loan EMI' }))

    expect(await screen.findByText('Monthly instalment')).toBeInTheDocument()
    expect(screen.getByText('Monthly instalment').nextSibling).toHaveTextContent('8,997.26')
  })

  it('shows the maturity value of a recurring deposit', async () => {
    renderWithMantine(<CalculatorPage />)

    const user = userEvent.setup()
    await user.click(screen.getByRole('tab', { name: 'Recurring deposit' }))

    expect(await screen.findByText('Total deposited')).toBeInTheDocument()
    expect(screen.getByText('Total deposited').nextSibling).toHaveTextContent('300,000.00')
  })

  it('asks for values instead of showing a result when a field is empty', async () => {
    renderWithMantine(<CalculatorPage />)

    const user = userEvent.setup()
    await user.clear(screen.getByLabelText('Amount'))

    expect(await screen.findByText('Enter an amount, rate and period to see the result')).toBeInTheDocument()
    expect(screen.queryByText('Maturity amount')).not.toBeInTheDocument()
  })

  it('says the figures are an illustration', () => {
    renderWithMantine(<CalculatorPage />)

    expect(screen.getByText(/illustration only/i)).toBeInTheDocument()
  })
})
