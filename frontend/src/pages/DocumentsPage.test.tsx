import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { clearSession, setSessionTokens } from '../auth/authSession'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function profile(role: 'EMPLOYEE' | 'ADMIN') {
  return {
    id: '3952a31e-12eb-47ef-acab-20ad7a566f65',
    firstName: 'Brian',
    lastName: 'Albert',
    email: 'brian@example.com',
    role,
  }
}

describe('document management routing', () => {
  afterEach(() => {
    clearSession()
    vi.unstubAllGlobals()
  })

  it('loads knowledge documents for an administrator', async () => {
    setSessionTokens({ accessToken: 'old-access', refreshToken: 'refresh-1' })
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ accessToken: 'admin-access', refreshToken: 'refresh-2' }),
      )
      .mockResolvedValueOnce(jsonResponse(profile('ADMIN')))
      .mockResolvedValueOnce(
        jsonResponse({
          content: [
            {
              id: 'b801ce28-6a6c-4217-94a8-cb9bfeaaea6d',
              displayName: 'Employee Handbook',
              originalFileName: 'employee-handbook.pdf',
              contentType: 'application/pdf',
              sizeBytes: 2048,
              status: 'READY',
              pageCount: 14,
              failureReason: null,
              uploadedByUserId: '3952a31e-12eb-47ef-acab-20ad7a566f65',
              createdAt: '2026-08-11T12:00:00Z',
              updatedAt: '2026-08-11T12:05:00Z',
            },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        }),
      )
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MemoryRouter initialEntries={['/app/documents']}>
        <App />
      </MemoryRouter>,
    )

    expect(
      await screen.findByRole('heading', { name: 'Documents become evidence.' }),
    ).toBeInTheDocument()
    expect(await screen.findByText('Employee Handbook')).toBeInTheDocument()
    expect(screen.getByText('14 pages')).toBeInTheDocument()
    expect(screen.getByText('Ready for questions')).toBeInTheDocument()

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3))
    expect(fetchMock.mock.calls[2][0]).toBe('/api/admin/documents?page=0&size=10')
  })

  it('keeps an employee out of the admin document screen', async () => {
    setSessionTokens({ accessToken: 'old-access', refreshToken: 'refresh-1' })
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ accessToken: 'employee-access', refreshToken: 'refresh-2' }),
      )
      .mockResolvedValueOnce(jsonResponse(profile('EMPLOYEE')))
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MemoryRouter initialEntries={['/app/documents']}>
        <App />
      </MemoryRouter>,
    )

    expect(
      await screen.findByRole('heading', { name: 'This area is for administrators.' }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Documents become evidence.' })).not.toBeInTheDocument()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
  })
})
