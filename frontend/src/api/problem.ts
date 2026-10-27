export interface FieldError {
  field: string
  message: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly errors: FieldError[]
  readonly correlationId?: string

  constructor(init: { status: number; code: string; message: string; errors?: FieldError[]; correlationId?: string }) {
    super(init.message)
    this.name = 'ApiError'
    this.status = init.status
    this.code = init.code
    this.errors = init.errors ?? []
    this.correlationId = init.correlationId
  }
}

interface ProblemBody {
  title?: unknown
  detail?: unknown
  code?: unknown
  correlationId?: unknown
  errors?: unknown
}

function asText(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value : undefined
}

function asFieldErrors(value: unknown): FieldError[] {
  if (!Array.isArray(value)) {
    return []
  }
  return value.flatMap((item: unknown) => {
    const entry = item as { field?: unknown; message?: unknown }
    const field = asText(entry?.field)
    const message = asText(entry?.message)
    return field && message ? [{ field, message }] : []
  })
}

export async function toApiError(response: Response): Promise<ApiError> {
  let body: ProblemBody = {}
  try {
    body = (await response.json()) as ProblemBody
  } catch {
    body = {}
  }
  return new ApiError({
    status: response.status,
    code: asText(body.code) ?? `HTTP_${response.status}`,
    message: asText(body.detail) ?? asText(body.title) ?? `Request failed with status ${response.status}`,
    errors: asFieldErrors(body.errors),
    correlationId: asText(body.correlationId),
  })
}
