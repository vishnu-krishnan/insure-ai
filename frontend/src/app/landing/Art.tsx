interface ArtProps {
  size?: number
}

export function HeroScene({ size = 460 }: ArtProps) {
  return (
    <svg width={size} height={size * 0.86} viewBox="0 0 460 396" fill="none" aria-hidden="true" focusable="false">
      <ellipse cx="230" cy="352" rx="190" ry="26" fill="#0b1f55" fillOpacity="0.28" />
      <circle cx="230" cy="190" r="150" fill="white" fillOpacity="0.09" />
      <circle cx="230" cy="190" r="108" fill="white" fillOpacity="0.1" />
      <path d="M230 52 L330 90 V190 C330 252 288 300 230 326 C172 300 130 252 130 190 V90 Z" fill="#12b886" />
      <path d="M230 52 L330 90 V190 C330 252 288 300 230 326 Z" fill="#0ca678" />
      <path
        d="M180 188 L218 226 L284 148"
        stroke="white"
        strokeWidth="18"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <rect x="40" y="226" width="96" height="124" rx="12" fill="white" />
      <rect x="54" y="246" width="52" height="8" rx="4" fill="#2557d6" />
      <rect x="54" y="266" width="68" height="6" rx="3" fill="#2557d6" fillOpacity="0.25" />
      <rect x="54" y="280" width="60" height="6" rx="3" fill="#2557d6" fillOpacity="0.25" />
      <rect x="54" y="294" width="66" height="6" rx="3" fill="#2557d6" fillOpacity="0.25" />
      <rect x="54" y="318" width="34" height="12" rx="6" fill="#f59f00" />
      <rect x="324" y="238" width="104" height="82" rx="14" fill="white" />
      <circle cx="350" cy="266" r="12" fill="#2557d6" fillOpacity="0.15" />
      <path d="M344 266 l5 5 9-10" stroke="#2557d6" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="368" y="258" width="46" height="7" rx="3.5" fill="#2557d6" fillOpacity="0.7" />
      <rect x="368" y="272" width="34" height="6" rx="3" fill="#2557d6" fillOpacity="0.25" />
      <rect x="340" y="296" width="72" height="8" rx="4" fill="#12b886" fillOpacity="0.8" />
      <circle cx="78" cy="96" r="7" fill="white" fillOpacity="0.5" />
      <circle cx="410" cy="120" r="9" fill="white" fillOpacity="0.35" />
      <circle cx="396" cy="354" r="5" fill="white" fillOpacity="0.45" />
    </svg>
  )
}

const tile = {
  width: 120,
  height: 96,
  viewBox: '0 0 120 96',
  fill: 'none',
  'aria-hidden': true,
  focusable: false,
} as const

export function AutoArt() {
  return (
    <svg {...tile}>
      <ellipse cx="60" cy="84" rx="46" ry="6" fill="#2557d6" fillOpacity="0.12" />
      <path
        d="M14 66 L20 46 C22 40 27 36 34 36 H80 C87 36 92 40 96 46 L106 58 C109 61 110 64 110 68 V72 H14 Z"
        fill="#2557d6"
      />
      <path d="M32 40 H62 V54 H24 Z M68 40 H80 C84 40 88 43 91 47 L96 54 H68 Z" fill="#dbe7ff" />
      <circle cx="36" cy="72" r="11" fill="#0f275f" />
      <circle cx="36" cy="72" r="5" fill="#dbe7ff" />
      <circle cx="90" cy="72" r="11" fill="#0f275f" />
      <circle cx="90" cy="72" r="5" fill="#dbe7ff" />
      <rect x="100" y="60" width="9" height="5" rx="2.5" fill="#f59f00" />
    </svg>
  )
}

export function HomeArt() {
  return (
    <svg {...tile}>
      <ellipse cx="60" cy="86" rx="44" ry="6" fill="#0ca678" fillOpacity="0.14" />
      <path d="M60 10 L108 48 H98 V84 H22 V48 H12 Z" fill="#12b886" />
      <path d="M60 10 L108 48 H60 Z" fill="#0ca678" />
      <rect x="48" y="56" width="24" height="28" rx="3" fill="white" />
      <rect x="28" y="54" width="14" height="14" rx="2" fill="#dbe7ff" />
      <rect x="78" y="54" width="14" height="14" rx="2" fill="#dbe7ff" />
      <circle cx="66" cy="70" r="2" fill="#0ca678" />
      <rect x="82" y="22" width="9" height="18" rx="2" fill="#0ca678" />
    </svg>
  )
}

export function LifeArt() {
  return (
    <svg {...tile}>
      <ellipse cx="60" cy="86" rx="44" ry="6" fill="#7048e8" fillOpacity="0.14" />
      <path d="M60 8 C30 8 12 28 12 52 H108 C108 28 90 8 60 8 Z" fill="#7048e8" />
      <path d="M60 8 C90 8 108 28 108 52 H60 Z" fill="#5f3dc4" />
      <rect x="57" y="52" width="6" height="26" fill="#3b2a8f" />
      <path d="M63 78 C63 85 52 85 52 79" stroke="#3b2a8f" strokeWidth="5" strokeLinecap="round" fill="none" />
      <path d="M60 36 C54 30 46 33 46 40 C46 47 60 54 60 54 C60 54 74 47 74 40 C74 33 66 30 60 36 Z" fill="white" />
    </svg>
  )
}

export function HealthArt() {
  return (
    <svg {...tile}>
      <ellipse cx="60" cy="86" rx="44" ry="6" fill="#e03131" fillOpacity="0.12" />
      <path d="M60 10 L100 24 V50 C100 68 83 80 60 88 C37 80 20 68 20 50 V24 Z" fill="#fa5252" />
      <path d="M60 10 L100 24 V50 C100 68 83 80 60 88 Z" fill="#e03131" />
      <rect x="53" y="30" width="14" height="40" rx="3" fill="white" />
      <rect x="40" y="43" width="40" height="14" rx="3" fill="white" />
    </svg>
  )
}
