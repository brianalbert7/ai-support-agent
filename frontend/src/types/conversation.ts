export type ConversationMessageRole = 'USER' | 'ASSISTANT'

export interface Conversation {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface KnowledgeCitation {
  sourceNumber: number
  chunkId: string
  documentId: string
  documentName: string
  pageNumber: number
  excerpt: string
  similarity: number
}

export interface ConversationMessage {
  id: string
  role: ConversationMessageRole
  content: string
  grounded: boolean
  citations: KnowledgeCitation[]
  createdAt: string
}

export interface ConversationExchange {
  userMessage: ConversationMessage
  assistantMessage: ConversationMessage
}
