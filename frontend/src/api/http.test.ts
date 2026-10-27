import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { z } from 'zod'

import { server } from '../test/server'
import { ApiError } from './problem'
import { request, setTokenProvider, setUnauthorizedHandler } from './http'

const pong = z.object({ ok: z.boolean() })

afterEach(() => {
  setTokenProvider(async () => null)
  setUnauthorizedHandler(() => {})
})

describe('request', () => {
  it('sends the bearer token and parses the response', async () => {
    setTokenProvider(async () => 'abc')
    let header: string | null = null
    server.use(
      http.get('/api/v1/ping', ({ request: req }) => {
        header = req.headers.get('authorization')
        return HttpResponse.json({ ok: true })
      }),
    )

    const result = await request('', '/api/v1/ping', pong)

    expect(result).toEqual({ ok: true })
    expect(header).toBe('Bearer abc')
  })

  it('notifies the unauthorized handler on 401', async () => {
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    server.use(http.get('/api/v1/ping', () => new HttpResponse(null, { status: 401 })))

    await expect(request('', '/api/v1/ping', pong)).rejects.toMatchObject({ status: 401 })
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it('turns a problem body into an ApiError with field errors', async () => {
    server.use(
      http.post('/api/v1/ping', () =>
        HttpResponse.json(
          {
            status: 400,
            title: 'Validation failed',
            code: 'VALIDATION_FAILED',
            correlationId: 'c-1',
            errors: [
              { field: 'email', message: 'email must be a valid email address' },
              { field: 'phone', message: 'phone must be in E.164 format' },
            ],
          },
          { status: 400, headers: { 'content-type': 'application/problem+json' } },
        ),
      ),
    )

    const failure = await request('', '/api/v1/ping', pong, { method: 'POST', body: '{}' }).catch(
      (error: unknown) => error,
    )

    expect(failure).toBeInstanceOf(ApiError)
    expect(failure).toMatchObject({
      status: 400,
      code: 'VALIDATION_FAILED',
      correlationId: 'c-1',
    })
    expect((failure as ApiError).errors).toHaveLength(2)
  })

  it('still produces an ApiError when the failure body is not json', async () => {
    server.use(http.get('/api/v1/ping', () => new HttpResponse('upstream down', { status: 502 })))

    await expect(request('', '/api/v1/ping', pong)).rejects.toMatchObject({
      status: 502,
      code: 'HTTP_502',
    })
  })
})
