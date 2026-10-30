export interface CoverPhotos {
  card: string
  side: string
  banner: string
  alt: string
  position: string
}

export const COVER_PHOTOS: Record<string, CoverPhotos> = {
  AUTO: {
    card: '/images/cover-auto.jpg',
    side: '/images/side-auto.jpg',
    banner: '/images/banner-auto.jpg',
    alt: 'A red coupe driving on a wet mountain road',
    position: 'center 60%',
  },
  HOME: {
    card: '/images/cover-home.jpg',
    side: '/images/side-home.jpg',
    banner: '/images/banner-home.jpg',
    alt: 'A house with lit windows at dusk',
    position: 'center 55%',
  },
  LIFE: {
    card: '/images/cover-life.jpg',
    side: '/images/side-life.jpg',
    banner: '/images/banner-life.jpg',
    alt: 'A family laughing together in the woods',
    position: 'center 35%',
  },
  HEALTH: {
    card: '/images/cover-health.jpg',
    side: '/images/side-health.jpg',
    banner: '/images/banner-health.jpg',
    alt: 'A family on a beach with their dog',
    position: 'center 45%',
  },
}
