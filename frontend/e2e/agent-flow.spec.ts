import { expect, test } from '@playwright/test'

const username = process.env.E2E_AGENT_USERNAME ?? 'agent1'
const password = process.env.E2E_AGENT_PASSWORD

test('agent signs in, creates a customer and a quote, and finds it in the list', async ({ page }) => {
  test.skip(!password, 'Set E2E_AGENT_PASSWORD to run the end-to-end flow')

  await page.goto('/')
  await page.getByLabel(/username or email/i).fill(username)
  await page.getByLabel('Password', { exact: true }).fill(password!)
  await page.getByRole('button', { name: /sign in/i }).click()
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()

  const unique = Date.now().toString().slice(-8)
  await page.getByRole('link', { name: 'Customers' }).click()
  await page.getByRole('link', { name: 'New customer' }).click()
  await page.getByLabel('Full name').fill('Asha Menon')
  await page.getByLabel('Email').fill(`asha.${unique}@example.com`)
  await page.getByLabel('Phone').fill(`+9198${unique}`)
  await page.getByLabel('Date of birth').fill('1990-04-12')
  await page.getByRole('button', { name: 'Create customer' }).click()
  await expect(page.getByRole('heading', { name: 'Asha Menon' })).toBeVisible()

  const customerNumber =
    (await page
      .getByText(/^CUS-\d{6,}$/)
      .first()
      .textContent()) ?? ''

  // Find the new customer again by name and by business ID.
  await page.getByRole('link', { name: 'Customers' }).click()
  await page.getByLabel('Search customers').fill(`asha.${unique}`)
  await expect(page.getByRole('link', { name: customerNumber })).toBeVisible()
  await page.getByLabel('Search customers').fill(customerNumber)
  await expect(page.getByRole('link', { name: customerNumber })).toBeVisible()
  await page.getByRole('link', { name: customerNumber }).click()

  await page.getByRole('link', { name: 'Create a quote' }).click()
  await expect(page.getByText('Selected customer')).toBeVisible()
  await page.getByLabel('Coverage type').selectOption('HOME')
  await page.getByLabel('Coverage amount').fill('250000')
  await page.getByRole('button', { name: 'Create quote' }).click()

  await expect(page.getByRole('heading', { level: 2, name: /^Quote QT-\d{6,}$/ })).toBeVisible()
  await expect(page.getByText(/\d{1,3}(,\d{3})*\.\d{2}/).first()).toBeVisible()
  const quoteNumber = ((await page.getByRole('heading', { level: 2 }).textContent()) ?? '').replace('Quote ', '').trim()

  await page.getByRole('link', { name: 'Quotes', exact: true }).click()
  await page.getByLabel('Search quotes').fill(customerNumber)
  await expect(page.getByRole('link', { name: quoteNumber })).toBeVisible()
})
