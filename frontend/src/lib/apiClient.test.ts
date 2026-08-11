import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './apiClient'

describe('apiRequest', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('uses the configured API boundary and parses JSON responses', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ email: 'employee@example.com' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const response = await apiRequest<{ email: string }>('/users/me')

    expect(response.email).toBe('employee@example.com')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/users/me',
      expect.objectContaining({ headers: expect.any(Headers) }),
    )
  })

  it('turns an unsuccessful API response into a typed error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ message: 'Authentication is required' }), {
          status: 401,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )

    await expect(apiRequest('/users/me')).rejects.toEqual(
      expect.objectContaining<ApiError>({
        name: 'ApiError',
        message: 'Authentication is required',
        status: 401,
        body: { message: 'Authentication is required' },
      }),
    )
  })
})
