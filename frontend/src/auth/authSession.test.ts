import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  authenticatedApiRequest,
  clearSession,
  getAccessToken,
  getRefreshToken,
  refreshAccessToken,
  setSessionTokens,
} from './authSession'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('authentication session', () => {
  afterEach(() => {
    clearSession()
    vi.unstubAllGlobals()
  })

  it('deduplicates simultaneous refresh-token rotations', async () => {
    setSessionTokens({ accessToken: 'old-access', refreshToken: 'refresh-1' })
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ accessToken: 'new-access', refreshToken: 'refresh-2' }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const [firstToken, secondToken] = await Promise.all([
      refreshAccessToken(),
      refreshAccessToken(),
    ])

    expect(firstToken).toBe('new-access')
    expect(secondToken).toBe('new-access')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(getAccessToken()).toBe('new-access')
    expect(getRefreshToken()).toBe('refresh-2')
  })

  it('refreshes after a 401 and retries the protected request once', async () => {
    setSessionTokens({ accessToken: 'expired-access', refreshToken: 'refresh-1' })
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ message: 'Access token expired' }, 401))
      .mockResolvedValueOnce(
        jsonResponse({ accessToken: 'new-access', refreshToken: 'refresh-2' }),
      )
      .mockResolvedValueOnce(jsonResponse({ email: 'employee@example.com' }))
    vi.stubGlobal('fetch', fetchMock)

    const profile = await authenticatedApiRequest<{ email: string }>('/users/me')

    expect(profile.email).toBe('employee@example.com')
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/refresh')
    expect(fetchMock.mock.calls[2][0]).toBe('/api/users/me')

    const retryHeaders = fetchMock.mock.calls[2][1]?.headers
    expect(retryHeaders).toBeInstanceOf(Headers)
    expect((retryHeaders as Headers).get('Authorization')).toBe('Bearer new-access')
    expect(getRefreshToken()).toBe('refresh-2')
  })

  it('clears browser credentials when refresh-token rotation fails', async () => {
    setSessionTokens({ accessToken: 'expired-access', refreshToken: 'invalid-refresh' })
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ message: 'Refresh token is invalid' }, 401),
      ),
    )

    await expect(refreshAccessToken()).rejects.toMatchObject({ status: 401 })

    expect(getAccessToken()).toBeNull()
    expect(getRefreshToken()).toBeNull()
  })
})
