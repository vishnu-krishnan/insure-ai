import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { Reveal } from './Reveal'

afterEach(() => vi.unstubAllGlobals())

describe('Reveal', () => {
  it('shows its content straight away when IntersectionObserver is unavailable', () => {
    vi.stubGlobal('IntersectionObserver', undefined)
    render(<Reveal>content</Reveal>)

    expect(screen.getByText('content')).toHaveAttribute('data-in', 'true')
  })

  it('waits until the content scrolls into view', () => {
    let trigger: (entries: { isIntersecting: boolean }[]) => void = () => {}
    vi.stubGlobal(
      'IntersectionObserver',
      class {
        constructor(callback: typeof trigger) {
          trigger = callback
        }
        observe() {}
        disconnect() {}
      },
    )
    render(<Reveal>content</Reveal>)
    expect(screen.getByText('content')).toHaveAttribute('data-in', 'false')

    act(() => trigger([{ isIntersecting: true }]))

    expect(screen.getByText('content')).toHaveAttribute('data-in', 'true')
  })
})
