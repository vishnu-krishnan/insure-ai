import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { renderWithMantine } from '../../test/render'
import { PhotoSlot } from './PhotoSlot'

describe('PhotoSlot', () => {
  it('shows the photo when it loads', () => {
    renderWithMantine(<PhotoSlot src="/images/a.jpg" alt="A family" fallback={<span>art</span>} />)

    expect(screen.getByRole('img', { name: 'A family' })).toHaveAttribute('src', '/images/a.jpg')
    expect(screen.queryByText('art')).not.toBeInTheDocument()
  })

  it('shows the illustration when the photo is missing', () => {
    renderWithMantine(<PhotoSlot src="/images/missing.jpg" alt="A family" fallback={<span>art</span>} />)

    fireEvent.error(screen.getByRole('img', { name: 'A family' }))

    expect(screen.getByText('art')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })
})
