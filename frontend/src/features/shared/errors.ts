import { ApiError } from '../../api/problem'

export interface ErrorSummary {
  message: string
  correlationId?: string
}

// Maps a failed call to text for the user. Known codes get a specific message;
// anything unexpected gets a generic one plus the correlation ID to quote.
export function describeError(error: unknown, known: Record<string, string> = {}): ErrorSummary {
  if (error instanceof ApiError) {
    const message = known[error.code]
    if (message) {
      return { message }
    }
    if (error.status === 401) {
      return { message: 'Your session has expired. Please sign in again.' }
    }
    if (error.status === 403) {
      return { message: 'You do not have permission to do this.' }
    }
    if (error.status >= 400 && error.status < 500 && error.code !== `HTTP_${error.status}`) {
      return { message: error.message, correlationId: error.correlationId }
    }
    return { message: 'Something went wrong on the server. Please try again.', correlationId: error.correlationId }
  }
  return { message: 'Could not reach the server. Check your connection and try again.' }
}
