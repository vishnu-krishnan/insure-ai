export function HeroArt({ size = 220 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 220 220" fill="none" aria-hidden="true" focusable="false">
      <circle cx="110" cy="110" r="96" fill="white" fillOpacity="0.08" />
      <circle cx="110" cy="110" r="68" fill="white" fillOpacity="0.1" />
      <rect x="52" y="64" width="84" height="104" rx="10" fill="white" fillOpacity="0.92" />
      <rect x="64" y="82" width="46" height="7" rx="3.5" fill="#2557d6" fillOpacity="0.85" />
      <rect x="64" y="98" width="60" height="5" rx="2.5" fill="#2557d6" fillOpacity="0.28" />
      <rect x="64" y="110" width="52" height="5" rx="2.5" fill="#2557d6" fillOpacity="0.28" />
      <rect x="64" y="122" width="58" height="5" rx="2.5" fill="#2557d6" fillOpacity="0.28" />
      <rect x="64" y="144" width="28" height="10" rx="5" fill="#12b886" />
      <path d="M150 52 L186 66 V108 C186 134 170 152 150 162 C130 152 114 134 114 108 V66 Z" fill="#12b886" />
      <path d="M150 52 L186 66 V108 C186 134 170 152 150 162 Z" fill="#0ca678" />
      <path d="M134 107 L146 119 L168 94" stroke="white" strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="40" cy="48" r="5" fill="white" fillOpacity="0.5" />
      <circle cx="190" cy="182" r="7" fill="white" fillOpacity="0.35" />
      <circle cx="26" cy="150" r="4" fill="white" fillOpacity="0.35" />
    </svg>
  )
}
