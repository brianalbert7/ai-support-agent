import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { clearSession, getRefreshToken } from './authSession'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('authentication flow', () => {
  afterEach(() => {
    clearSession()
    vi.unstubAllGlobals()
  })

  it('logs in, loads the current user, and enters the protected workspace', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ accessToken: 'access-token', refreshToken: 'refresh-token' }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          id: 'd116e8f6-f560-42ae-bcc6-f7442c10631e',
          firstName: 'Brian',
          lastName: 'Albert',
          email: 'brian@example.com',
          role: 'EMPLOYEE',
        }),
      )
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MemoryRouter initialEntries={['/login']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Welcome back.' })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Email address'), {
      target: { value: 'brian@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'correct-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Sign in securely' }))

    expect(
      await screen.findByRole('heading', { name: 'Your knowledge workspace.' }),
    ).toBeInTheDocument()
    expect(screen.getByText('brian@example.com')).toBeInTheDocument()
    expect(getRefreshToken()).toBe('refresh-token')

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    expect(fetchMock.mock.calls[0][0]).toBe('/api/auth/login')
    expect(fetchMock.mock.calls[1][0]).toBe('/api/users/me')
  })

  it('renders field validation returned by Spring', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          status: 400,
          code: 'VALIDATION_FAILED',
          message: 'Request validation failed',
          fieldErrors: { email: 'Email must be valid' },
        },
        400,
      ),
    )
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MemoryRouter initialEntries={['/register']}>
        <App />
      </MemoryRouter>,
    )

    expect(
      await screen.findByRole('heading', { name: 'Create your account.' }),
    ).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('First name'), { target: { value: 'Brian' } })
    fireEvent.change(screen.getByLabelText('Last name'), { target: { value: 'Albert' } })
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: 'invalid' } })
    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'correct-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create employee account' }))

    expect(await screen.findByText('Email must be valid')).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('Request validation failed')
  })
})
