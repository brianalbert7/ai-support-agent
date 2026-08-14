import { authenticatedApiRequest } from '../auth/authSession'
import type { PagedResponse } from '../types/api'
import type {
  Conversation,
  ConversationExchange,
  ConversationMessage,
} from '../types/conversation'

function conversationPath(conversationId: string): string {
  return `/conversations/${encodeURIComponent(conversationId)}`
}

export function listConversations(
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<PagedResponse<Conversation>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  return authenticatedApiRequest<PagedResponse<Conversation>>(
    `/conversations?${query}`,
    { signal },
  )
}

export function createConversation(title: string): Promise<Conversation> {
  return authenticatedApiRequest<Conversation>('/conversations', {
    method: 'POST',
    body: JSON.stringify({ title }),
  })
}

export function getConversation(
  conversationId: string,
  signal?: AbortSignal,
): Promise<Conversation> {
  return authenticatedApiRequest<Conversation>(conversationPath(conversationId), { signal })
}

export function listConversationMessages(
  conversationId: string,
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<PagedResponse<ConversationMessage>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  return authenticatedApiRequest<PagedResponse<ConversationMessage>>(
    `${conversationPath(conversationId)}/messages?${query}`,
    { signal },
  )
}

export function askConversation(
  conversationId: string,
  question: string,
  maxResults = 5,
): Promise<ConversationExchange> {
  return authenticatedApiRequest<ConversationExchange>(
    `${conversationPath(conversationId)}/messages`,
    {
      method: 'POST',
      body: JSON.stringify({ question, maxResults }),
    },
  )
}
