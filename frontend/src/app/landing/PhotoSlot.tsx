import { Box } from '@mantine/core'
import { useState, type ReactNode } from 'react'

interface PhotoSlotProps {
  src: string
  alt: string
  fallback: ReactNode
  height?: number | string
  radius?: string
}

// Shows a photo from /images when it exists, and the illustration when it does not.
export function PhotoSlot({ src, alt, fallback, height = 140, radius = 'var(--mantine-radius-md)' }: PhotoSlotProps) {
  const [failed, setFailed] = useState(false)
  return (
    <Box h={height} style={{ borderRadius: radius, overflow: 'hidden', position: 'relative' }}>
      {failed ? (
        fallback
      ) : (
        <img
          src={src}
          alt={alt}
          loading="lazy"
          onError={() => setFailed(true)}
          style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
        />
      )}
    </Box>
  )
}
