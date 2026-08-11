import type { AuthenticationTokens } from '../types/auth'
import { ApiError, apiRequest } from '../lib/apiClient'

const REFRESH_TOKEN_KEY = 'ai-support-agent.refresh-token'

export const AUTH_SESSION_EXPIRED_EVENT = 'ai-support-agent:session-expired'

let accessToken: string | null = null
let refreshInFlight: Promise<string> | null = null

export function setSessionTokens(tokens: AuthenticationTokens): void {
  accessToken = tokens.accessToken
  window.sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
}

export function getAccessToken(): string | null {
  return accessToken
}

export function getRefreshToken(): string | null {
  return window.sessionStorage.getItem(REFRESH_TOKEN_KEY)
}

export function hasRefreshToken(): boolean {
  return getRefreshToken() !== null
}

export function clearSession(): void {
  accessToken = null
  window.sessionStorage.removeItem(REFRESH_TOKEN_KEY)
}

function notifySessionExpired(): void {
  window.dispatchEvent(new Event(AUTH_SESSION_EXPIRED_EVENT))
}

async function rotateRefreshToken(): Promise<string> {
  const currentRefreshToken = getRefreshToken()
  if (currentRefreshToken === null) {
    throw new Error('No refresh token is available')
  }

  const tokens = await apiRequest<AuthenticationTokens>('/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken: currentRefreshToken }),
  })
  setSessionTokens(tokens)
  return tokens.accessToken
}

export function refreshAccessToken(): Promise<string> {
  if (refreshInFlight !== null) {
    return refreshInFlight
  }

  refreshInFlight = rotateRefreshToken()
    .catch((error: unknown) => {
      clearSession()
      notifySessionExpired()
      throw error
    })
    .finally(() => {
      refreshInFlight = null
    })

  return refreshInFlight
}

function withBearerToken(options: RequestInit, token: string): RequestInit {
  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${token}`)

  return { ...options, headers }
}

export async function authenticatedApiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = accessToken ?? (await refreshAccessToken())

  try {
    return await apiRequest<T>(path, withBearerToken(options, token))
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) {
      throw error
    }

    const refreshedAccessToken = await refreshAccessToken()
    return apiRequest<T>(path, withBearerToken(options, refreshedAccessToken))
  }
}
