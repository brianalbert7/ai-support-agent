import { ApiError } from './apiClient'

interface ApiErrorBody {
  message?: unknown
  fieldErrors?: unknown
}

function isApiErrorBody(body: unknown): body is ApiErrorBody {
  return typeof body === 'object' && body !== null
}

export function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

export function getFieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError) || !isApiErrorBody(error.body)) {
    return {}
  }

  const { fieldErrors } = error.body
  if (typeof fieldErrors !== 'object' || fieldErrors === null) {
    return {}
  }

  return Object.fromEntries(
    Object.entries(fieldErrors).filter(
      (entry): entry is [string, string] => typeof entry[1] === 'string',
    ),
  )
}
