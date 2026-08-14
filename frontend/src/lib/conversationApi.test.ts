import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearSession, setSessionTokens } from '../auth/authSession'
import { askConversation, listConversationMessages, listConversations } from './conversationApi'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('conversation API', () => {
  afterEach(() => {
    clearSession()
    vi.unstubAllGlobals()
  })

  it('uses the paginated owned-conversation and history endpoints', async () => {
    setSessionTokens({ accessToken: 'employee-access', refreshToken: 'employee-refresh' })
    const fetchMock = vi.fn().mockImplementation(() => (
      Promise.resolve(jsonResponse({ content: [] }))
    ))
    vi.stubGlobal('fetch', fetchMock)

    await listConversations(2, 12)
    await listConversationMessages('conversation/with spaces', 1, 100)

    expect(fetchMock.mock.calls[0][0]).toBe('/api/conversations?page=2&size=12')
    expect(fetchMock.mock.calls[1][0]).toBe(
      '/api/conversations/conversation%2Fwith%20spaces/messages?page=1&size=100',
    )
  })

  it('sends a grounded question with authentication and the retrieval limit', async () => {
    setSessionTokens({ accessToken: 'employee-access', refreshToken: 'employee-refresh' })
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      userMessage: { id: 'user-message' },
      assistantMessage: { id: 'assistant-message' },
    }, 201))
    vi.stubGlobal('fetch', fetchMock)

    await askConversation('conversation-1', 'How long are backups retained?')

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/conversations/conversation-1/messages')
    expect(options).toEqual(expect.objectContaining({ method: 'POST' }))
    expect(JSON.parse(String(options?.body))).toEqual({
      question: 'How long are backups retained?',
      maxResults: 5,
    })
    expect((options?.headers as Headers).get('Authorization')).toBe('Bearer employee-access')
  })
})
