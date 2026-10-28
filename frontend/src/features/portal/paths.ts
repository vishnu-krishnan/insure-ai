import { COVER_GUIDES } from '../insurance/catalogue'

export function portalPath(type: string): string {
  const slug = COVER_GUIDES.find((guide) => guide.code === type)?.slug ?? type.toLowerCase()
  return `/agent/${slug}`
}
