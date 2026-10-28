import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

const customerService = process.env.VITE_CUSTOMER_PROXY ?? 'http://localhost:8282'
const quoteService = process.env.VITE_QUOTE_PROXY ?? 'http://localhost:8281'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 3174,
    strictPort: true,
    proxy: {
      '/api/v1/customers': customerService,
      '/api/v1/quotes': quoteService,
      '/api/v2/quotes': quoteService,
    },
  },
  test: {
    environment: 'jsdom',
    env: { VITE_AUTH_MODE: 'mock' },
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    // The country lists make form pages heavy to render in jsdom.
    testTimeout: 20000,
  },
})
