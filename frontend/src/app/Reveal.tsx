import { useEffect, useRef, useState, type ReactNode } from 'react'

// Fades its children in the first time they scroll into view. Without
// IntersectionObserver, or with reduced motion, the content is simply shown.
export function Reveal({ children, delay = 0 }: { children: ReactNode; delay?: number }) {
  const ref = useRef<HTMLDivElement>(null)
  const [shown, setShown] = useState(typeof IntersectionObserver === 'undefined')

  useEffect(() => {
    const node = ref.current
    if (shown || !node) {
      return
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          setShown(true)
          observer.disconnect()
        }
      },
      { threshold: 0.12 },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [shown])

  return (
    <div
      ref={ref}
      className="reveal"
      data-in={shown}
      style={{ ['--reveal-delay' as string]: `${delay}ms`, height: '100%' }}
    >
      {children}
    </div>
  )
}
