import { IconCar, IconHeartbeat, IconHome2, IconUmbrella, type Icon } from '@tabler/icons-react'

export interface CoverageStyle {
  label: string
  color: string
  icon: Icon
}

// One colour and icon per cover type, used everywhere a cover type appears.
export const COVERAGE_STYLES: Record<string, CoverageStyle> = {
  AUTO: { label: 'Auto', color: 'brand', icon: IconCar },
  HOME: { label: 'Home', color: 'teal', icon: IconHome2 },
  LIFE: { label: 'Life', color: 'violet', icon: IconUmbrella },
  HEALTH: { label: 'Health', color: 'red', icon: IconHeartbeat },
}

export function coverageStyle(type: string): CoverageStyle {
  return COVERAGE_STYLES[type] ?? { label: type, color: 'gray', icon: IconUmbrella }
}
