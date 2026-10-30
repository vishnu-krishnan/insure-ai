import { Box } from '@mantine/core'

interface CoverPhotoProps {
  src: string
  alt: string
  height: number | string
  position?: string
  radius?: string
}

export function CoverPhoto({ src, alt, height, position = 'center', radius }: CoverPhotoProps) {
  return (
    <Box h={height} style={{ borderRadius: radius, overflow: 'hidden', background: 'var(--mantine-color-dark-6)' }}>
      <img
        src={src}
        alt={alt}
        loading="lazy"
        decoding="async"
        style={{ width: '100%', height: '100%', objectFit: 'cover', objectPosition: position, display: 'block' }}
      />
    </Box>
  )
}
