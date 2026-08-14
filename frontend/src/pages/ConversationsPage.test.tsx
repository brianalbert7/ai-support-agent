import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { clearSession, setSessionTokens } from '../auth/authSession'

const CONVERSATION_ID = 'ae829eba-59ec-4f42-a832-903dc193366c'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function paged(content: unknown[]) {
  return {
    content,
    page: 0,
    size: content.length || 12,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
  }
}

function message(
  id: string,
  role: 'USER' | 'ASSISTANT',
  content: string,
  grounded = false,
  citations: unknown[] = [],
) {
  return {
    id,
    role,
    content,
    grounded,
    citations,
    createdAt: '2026-08-14T01:11:15Z',
  }
}

describe('conversation workspace', () => {
  afterEach(() => {
    clearSession()
    vi.unstubAllGlobals()
  })

  it('creates a named conversation and opens its empty history', async () => {
    setSessionTokens({ accessToken: 'old-access', refreshToken: 'refresh-1' })
    const conversation = {
      id: CONVERSATION_ID,
      title: 'CloudDesk recovery demo',
      createdAt: '2026-08-14T01:09:23Z',
      updatedAt: '2026-08-14T01:09:23Z',
    }
    let conversationListCalls = 0
    const fetchMock = vi.fn((input: RequestInfo | URL, options?: RequestInit) => {
      const url = String(input)
      if (url === '/api/auth/refresh') {
        return Promise.resolve(jsonResponse({ accessToken: 'employee-access', refreshToken: 'refresh-2' }))
      }
      if (url === '/api/users/me') {
        return Promise.resolve(jsonResponse({
          id: '3952a31e-12eb-47ef-acab-20ad7a566f65',
          firstName: 'Brian',
          lastName: 'Albert',
          email: 'brian@example.com',
          role: 'EMPLOYEE',
        }))
      }
      if (url === '/api/conversations?page=0&size=12') {
        const content = conversationListCalls === 0 ? [] : [conversation]
        conversationListCalls += 1
        return Promise.resolve(jsonResponse(paged(content)))
      }
      if (url === '/api/conversations' && options?.method === 'POST') {
        return Promise.resolve(jsonResponse(conversation, 201))
      }
      if (url === `/api/conversations/${CONVERSATION_ID}`) {
        return Promise.resolve(jsonResponse(conversation))
      }
      if (url === `/api/conversations/${CONVERSATION_ID}/messages?page=0&size=100`) {
        return Promise.resolve(jsonResponse(paged([])))
      }
      return Promise.reject(new Error(`Unexpected request: ${url}`))
    })
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MemoryRouter initialEntries={['/app/conversations']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Conversations' })).toBeInTheDocument()
    expect(await screen.findByText('Create your first conversation to start asking grounded questions.'))
      .toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Start a conversation'), {
      target: { value: '  CloudDesk recovery demo  ' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create' }))

    expect(
      await screen.findByRole('heading', { name: 'CloudDesk recovery demo' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Ask the first question.' })).toBeInTheDocument()

    const createCall = fetchMock.mock.calls.find(
      ([url, options]) => String(url) === '/api/conversations' && options?.method === 'POST',
    )
    expect(JSON.parse(String(createCall?.[1]?.body))).toEqual({
      title: 'CloudDesk recovery demo',
    })
  })

  it('loads persisted history, renders evidence, and appends a follow-up exchange', async () => {
    setSessionTokens({ accessToken: 'old-access', refreshToken: 'refresh-1' })
    const conversation = {
      id: CONVERSATION_ID,
      title: 'CloudDesk recovery demo',
      createdAt: '2026-08-14T01:09:23Z',
      updatedAt: '2026-08-14T01:11:15Z',
    }
    const citation = {
      sourceNumber: 1,
      chunkId: 'chunk-1',
      documentId: 'document-1',
      documentName: 'clouddesk-administrator-guide',
      pageNumber: 2,
      excerpt: 'Daily backups are retained for 35 days.',
      similarity: 0.6178,
    }
    const originalMessages = [
      message('message-1', 'USER', 'How long are CloudDesk backups retained?'),
      message(
        'message-2',
        'ASSISTANT',
        'Daily backups are retained for 35 days [1].',
        true,
        [citation],
      ),
    ]
    const followUpExchange = {
      userMessage: message(
        'message-3',
        'USER',
        'Who is allowed to request one?',
      ),
      assistantMessage: message(
        'message-4',
        'ASSISTANT',
        'Only a Workspace Administrator may request a restore [1].',
        true,
        [citation],
      ),
    }

    const fetchMock = vi.fn((input: RequestInfo | URL, options?: RequestInit) => {
      const url = String(input)
      if (url === '/api/auth/refresh') {
        return Promise.resolve(jsonResponse({ accessToken: 'employee-access', refreshToken: 'refresh-2' }))
      }
      if (url === '/api/users/me') {
        return Promise.resolve(jsonResponse({
          id: '3952a31e-12eb-47ef-acab-20ad7a566f65',
          firstName: 'Brian',
          lastName: 'Albert',
          email: 'brian@example.com',
          role: 'EMPLOYEE',
        }))
      }
      if (url === '/api/conversations?page=0&size=12') {
        return Promise.resolve(jsonResponse(paged([conversation])))
      }
      if (url === `/api/conversations/${CONVERSATION_ID}`) {
        return Promise.resolve(jsonResponse(conversation))
      }
      if (url === `/api/conversations/${CONVERSATION_ID}/messages?page=0&size=100`) {
        return Promise.resolve(jsonResponse(paged(originalMessages)))
      }
      if (url === `/api/conversations/${CONVERSATION_ID}/messages` && options?.method === 'POST') {
        return Promise.resolve(jsonResponse(followUpExchange, 201))
      }
      return Promise.reject(new Error(`Unexpected request: ${url}`))
    })
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MemoryRouter initialEntries={[`/app/conversations/${CONVERSATION_ID}`]}>
        <App />
      </MemoryRouter>,
    )

    expect(
      await screen.findByRole('heading', { name: 'CloudDesk recovery demo' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Daily backups are retained for 35 days [1].')).toBeInTheDocument()
    expect(screen.getByText('Grounded answer')).toBeInTheDocument()
    expect(screen.getByText('clouddesk-administrator-guide')).toBeInTheDocument()
    expect(screen.getByText('Page 2')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Ask the knowledge base'), {
      target: { value: 'Who is allowed to request one?' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Ask question' }))

    expect(
      await screen.findByText('Only a Workspace Administrator may request a restore [1].'),
    ).toBeInTheDocument()
    expect(screen.getByText('4 messages')).toBeInTheDocument()

    await waitFor(() => {
      const askCall = fetchMock.mock.calls.find(
        ([url, options]) => String(url).endsWith(`/${CONVERSATION_ID}/messages`) && options?.method === 'POST',
      )
      expect(askCall).toBeDefined()
      expect(JSON.parse(String(askCall?.[1]?.body))).toEqual({
        question: 'Who is allowed to request one?',
        maxResults: 5,
      })
    })
  })
})
