import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearSession, setSessionTokens } from '../auth/authSession'
import { uploadDocument } from './documentApi'

describe('document API', () => {
  afterEach(() => {
    clearSession()
    vi.unstubAllGlobals()
  })

  it('uploads PDF bytes as multipart form data with authentication', async () => {
    setSessionTokens({ accessToken: 'admin-access', refreshToken: 'admin-refresh' })
    const responseBody = {
      id: 'b801ce28-6a6c-4217-94a8-cb9bfeaaea6d',
      displayName: 'Employee Handbook',
      originalFileName: 'handbook.pdf',
      contentType: 'application/pdf',
      sizeBytes: 12,
      status: 'UPLOADED',
      pageCount: null,
      failureReason: null,
      uploadedByUserId: '3952a31e-12eb-47ef-acab-20ad7a566f65',
      createdAt: '2026-08-11T12:00:00Z',
      updatedAt: '2026-08-11T12:00:00Z',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(responseBody), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const file = new File(['pdf-content'], 'handbook.pdf', { type: 'application/pdf' })
    const response = await uploadDocument('Employee Handbook', file)

    expect(response.status).toBe('UPLOADED')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/admin/documents')

    const formData = options?.body
    expect(formData).toBeInstanceOf(FormData)
    expect((formData as FormData).get('displayName')).toBe('Employee Handbook')
    expect((formData as FormData).get('file')).toBe(file)

    const headers = options?.headers
    expect(headers).toBeInstanceOf(Headers)
    expect((headers as Headers).get('Authorization')).toBe('Bearer admin-access')
    expect((headers as Headers).has('Content-Type')).toBe(false)
  })
})
