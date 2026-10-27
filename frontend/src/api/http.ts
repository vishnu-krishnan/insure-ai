import type { ZodType } from 'zod'

import { toApiError } from './problem'

export type TokenProvider = () => Promise<string | null>

let tokenProvider: TokenProvider = async () => null
let unauthorizedHandler: () => void = () => {}

export function setTokenProvider(provider: TokenProvider): void {
  tokenProvider = provider
}

export function setUnauthorizedHandler(handler: () => void): void {
  unauthorizedHandler = handler
}

export async function request<T>(base: string, path: string, schema: ZodType<T>, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = await tokenProvider()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const url = new URL(`${base}${path}`, window.location.origin)
  const response = await fetch(url, { ...init, headers })

  if (!response.ok) {
    if (response.status === 401) {
      unauthorizedHandler()
    }
    throw await toApiError(response)
  }
  return schema.parse(await response.json())
}
