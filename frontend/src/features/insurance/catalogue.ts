export interface CoverGuide {
  code: 'AUTO' | 'HOME' | 'LIFE' | 'HEALTH'
  slug: string
  name: string
  tagline: string
  summary: string
  protects: string
  premiumBasis: string
  bestFor: string
  whoFor: string[]
  covered: string[]
  notCovered: string[]
  faqs: { q: string; a: string }[]
}

// Typical, general-purpose descriptions. The policy document that goes with a quote is what binds.
export const COVER_GUIDES: CoverGuide[] = [
  {
    code: 'AUTO',
    slug: 'auto',
    name: 'Auto insurance',
    tagline: 'Cover for the car and the people in it.',
    summary:
      'Auto insurance helps pay for damage to your vehicle and for the costs that follow an accident, so a bad day on the road does not become a financial one.',
    protects: 'Your vehicle and your liability on the road',
    premiumBasis: '7% of the cover amount a year',
    bestFor: 'Anyone who drives or owns a vehicle',
    whoFor: ['Car and bike owners', 'New drivers building a record', 'Families with more than one vehicle'],
    covered: [
      'Accidental damage to your own vehicle',
      'Theft of the vehicle',
      'Fire, flood and other natural events',
      'Third-party injury and property damage',
    ],
    notCovered: [
      'Driving without a valid licence',
      'Damage while under the influence of alcohol or drugs',
      'Normal wear and tear and mechanical failure',
      'Use outside the purpose declared in the policy',
    ],
    faqs: [
      {
        q: 'How is the cover amount chosen?',
        a: 'It is usually set to the value of the vehicle. A higher amount means a higher premium.',
      },
      {
        q: 'Can I lower my premium?',
        a: 'A higher deductible, which is the part you pay on a claim, normally lowers the premium.',
      },
    ],
  },
  {
    code: 'HOME',
    slug: 'home',
    name: 'Home insurance',
    tagline: 'Protection for the home and what is inside it.',
    summary:
      'Home insurance protects the building and your belongings against loss and damage, whether you own the home or rent it.',
    protects: 'Your property and belongings',
    premiumBasis: '5% of the cover amount a year',
    bestFor: 'Home owners and tenants',
    whoFor: [
      'Home owners with a mortgage',
      'Tenants protecting their belongings',
      'Landlords insuring a rented property',
    ],
    covered: [
      'Fire, lightning and explosion',
      'Flood, storm and other natural events',
      'Burglary and theft of contents',
      'Accidental damage to fixtures',
    ],
    notCovered: [
      'Gradual damage from poor maintenance',
      'Loss caused by deliberate acts',
      'War and nuclear risks',
      'Items above the stated limit unless listed',
    ],
    faqs: [
      {
        q: 'Does it cover my belongings?',
        a: 'Contents cover is chosen when you set the cover amount. Make sure it includes what you want protected.',
      },
      {
        q: 'I rent my home. Do I need it?',
        a: 'Your landlord insures the building, not your belongings. Contents cover protects those.',
      },
    ],
  },
  {
    code: 'LIFE',
    slug: 'life',
    name: 'Life insurance',
    tagline: 'A financial safety net for the family.',
    summary:
      'Life insurance pays a sum to the people you name if you are no longer there, helping them keep up with bills, education and everyday life.',
    protects: 'The financial future of your family',
    premiumBasis: '9% of the cover amount a year',
    bestFor: 'People with dependants or loans',
    whoFor: ['Parents and guardians', 'Anyone with a home loan', 'The main earner in a household'],
    covered: [
      'A payout to your nominee on death during the term',
      'Cover for the policy term you choose',
      'Option to pay monthly, quarterly or yearly',
    ],
    notCovered: [
      'Death from a condition you did not disclose',
      'Claims inside any exclusion period in the policy',
      'Self-inflicted harm during the early policy period',
    ],
    faqs: [
      {
        q: 'How much cover do I need?',
        a: 'A common rule of thumb is a multiple of yearly income plus any loans. An agent can help you work it out.',
      },
      { q: 'Who receives the payout?', a: 'The nominee recorded on the customer profile and the policy.' },
    ],
  },
  {
    code: 'HEALTH',
    slug: 'health',
    name: 'Health insurance',
    tagline: 'Support when medical costs arrive.',
    summary: 'Health insurance helps with hospital and treatment costs so a medical event does not drain your savings.',
    protects: 'You and your family against medical costs',
    premiumBasis: '8% of the cover amount a year',
    bestFor: 'Individuals and families of any age',
    whoFor: [
      'Families who want planned protection',
      'Self-employed people without employer cover',
      'Parents and older relatives',
    ],
    covered: [
      'Hospital stays and surgery',
      'Treatment before and after hospital admission',
      'Day-care procedures',
      'Ambulance charges',
    ],
    notCovered: [
      'Cosmetic procedures',
      'Conditions that existed before the policy unless declared and accepted',
      'Treatment outside the policy waiting periods',
      'Experimental treatments',
    ],
    faqs: [
      {
        q: 'Are existing conditions covered?',
        a: 'Existing conditions are usually covered only after a waiting period. Declare them when you apply.',
      },
      {
        q: 'Does age change the price?',
        a: 'Yes. Cover for customers over 60 costs more, and cover is offered from age 18 to 75.',
      },
    ],
  },
]

export const GUIDE_QUESTIONS = [
  { question: 'I drive or own a vehicle', answer: 'Auto insurance', slug: 'auto' },
  { question: 'I own or rent a home', answer: 'Home insurance', slug: 'home' },
  { question: 'Others depend on my income', answer: 'Life insurance', slug: 'life' },
  { question: 'I want help with medical bills', answer: 'Health insurance', slug: 'health' },
]

export function findGuide(slug: string | undefined): CoverGuide | undefined {
  return COVER_GUIDES.find((guide) => guide.slug === slug)
}
