import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  askConversation,
  createConversation,
  getConversation,
  listConversationMessages,
  listConversations,
} from '../lib/conversationApi'
import { getErrorMessage } from '../lib/apiError'
import type { PagedResponse } from '../types/api'
import type { Conversation, ConversationMessage, KnowledgeCitation } from '../types/conversation'

const CONVERSATION_PAGE_SIZE = 12
const HISTORY_PAGE_SIZE = 100

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(new Date(value))
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat('en-US', {
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(value))
}

function CitationCard({ citation }: { citation: KnowledgeCitation }) {
  return (
    <details className="citation-card">
      <summary>
        <span className="citation-number">[{citation.sourceNumber}]</span>
        <span className="citation-title">
          <strong>{citation.documentName}</strong>
          <small>Page {citation.pageNumber}</small>
        </span>
        <span className="citation-toggle">View evidence</span>
      </summary>
      <div className="citation-evidence">
        <p>{citation.excerpt}</p>
        <small>{Math.round(citation.similarity * 100)}% retrieval similarity</small>
      </div>
    </details>
  )
}

function MessageCard({ message }: { message: ConversationMessage }) {
  const assistant = message.role === 'ASSISTANT'

  return (
    <article className={`message-card ${assistant ? 'assistant-message' : 'user-message'}`}>
      <header>
        <strong>{assistant ? 'Knowledge assistant' : 'You'}</strong>
        <span>{formatTime(message.createdAt)}</span>
      </header>
      <p className="message-content">{message.content}</p>

      {assistant && (
        <div className="message-grounding">
          <span className={`grounding-badge ${message.grounded ? 'grounded' : 'not-grounded'}`}>
            {message.grounded ? 'Grounded answer' : 'Knowledge gap'}
          </span>
          {message.citations.length > 0 && (
            <span>{message.citations.length} {message.citations.length === 1 ? 'source' : 'sources'}</span>
          )}
        </div>
      )}

      {message.citations.length > 0 && (
        <div className="citation-list" aria-label="Answer sources">
          {message.citations.map((citation) => (
            <CitationCard key={`${citation.chunkId}-${citation.sourceNumber}`} citation={citation} />
          ))}
        </div>
      )}
    </article>
  )
}

interface ConversationDetailProps {
  conversationId: string
  onConversationUpdated: () => void
}

function ConversationDetail({ conversationId, onConversationUpdated }: ConversationDetailProps) {
  const messageEndRef = useRef<HTMLDivElement>(null)
  const [activeConversation, setActiveConversation] = useState<Conversation | null>(null)
  const [messagePage, setMessagePage] = useState<PagedResponse<ConversationMessage> | null>(null)
  const [historyLoading, setHistoryLoading] = useState(true)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [question, setQuestion] = useState('')
  const [questionError, setQuestionError] = useState<string | null>(null)
  const [asking, setAsking] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    Promise.all([
      getConversation(conversationId, controller.signal),
      listConversationMessages(conversationId, 0, HISTORY_PAGE_SIZE, controller.signal),
    ])
      .then(([conversation, history]) => {
        setActiveConversation(conversation)
        setMessagePage(history)
      })
      .catch((error: unknown) => {
        if (!isAbortError(error)) {
          setHistoryError(getErrorMessage(error, 'Unable to load this conversation.'))
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setHistoryLoading(false)
      })

    return () => controller.abort()
  }, [conversationId])

  useEffect(() => {
    const messageEnd = messageEndRef.current
    if (typeof messageEnd?.scrollIntoView === 'function') {
      messageEnd.scrollIntoView({ behavior: 'smooth', block: 'end' })
    }
  }, [messagePage?.content.length])

  async function handleAsk(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedQuestion = question.trim()
    if (trimmedQuestion === '') {
      setQuestionError('Enter a question for the knowledge base.')
      return
    }

    setAsking(true)
    setQuestionError(null)
    try {
      const exchange = await askConversation(conversationId, trimmedQuestion)
      setQuestion('')
      setMessagePage((current) => current === null ? current : {
        ...current,
        content: [...current.content, exchange.userMessage, exchange.assistantMessage],
        totalElements: current.totalElements + 2,
      })
      onConversationUpdated()
    } catch (error) {
      setQuestionError(getErrorMessage(error, 'Unable to generate an answer.'))
    } finally {
      setAsking(false)
    }
  }

  if (historyLoading) {
    return <div className="conversation-panel-state" aria-live="polite">Loading conversation…</div>
  }

  if (historyError !== null || activeConversation === null) {
    return (
      <div className="conversation-panel-state error-state">
        <p>{historyError ?? 'Unable to load this conversation.'}</p>
        <Link className="secondary-button" to="/app/conversations">Back to conversations</Link>
      </div>
    )
  }

  const messages = messagePage?.content ?? []

  return (
    <>
      <header className="conversation-header">
        <div>
          <p className="card-kicker">Grounded conversation</p>
          <h2>{activeConversation.title}</h2>
        </div>
        <span>{messagePage?.totalElements ?? 0} messages</span>
      </header>

      <div className="message-stream" role="log" aria-live="polite" aria-label="Conversation history">
        {messages.length === 0 ? (
          <div className="empty-conversation">
            <span aria-hidden="true">01</span>
            <h3>Ask the first question.</h3>
            <p>Try a specific policy, product, or support question covered by a ready PDF.</p>
          </div>
        ) : (
          messages.map((message) => <MessageCard key={message.id} message={message} />)
        )}
        {messagePage !== null && messagePage.totalElements > messagePage.content.length && (
          <p className="history-limit-note">
            Showing the first {messagePage.content.length} of {messagePage.totalElements} messages.
          </p>
        )}
        <div ref={messageEndRef} aria-hidden="true" />
      </div>

      <form className="question-form" onSubmit={handleAsk} noValidate>
        {questionError !== null && <div className="form-alert" role="alert">{questionError}</div>}
        <label htmlFor="knowledge-question">Ask the knowledge base</label>
        <textarea
          id="knowledge-question"
          maxLength={2000}
          rows={3}
          value={question}
          placeholder="How long are CloudDesk backups retained?"
          onChange={(event) => {
            setQuestion(event.target.value)
            setQuestionError(null)
          }}
          disabled={asking}
        />
        <div>
          <small>Answers are limited to retrieved company documents.</small>
          <button className="submit-button" type="submit" disabled={asking}>
            {asking ? 'Searching evidence…' : 'Ask question'}
          </button>
        </div>
      </form>
    </>
  )
}

export default function ConversationsPage() {
  const { conversationId } = useParams<{ conversationId: string }>()
  const navigate = useNavigate()
  const [listPage, setListPage] = useState(0)
  const [listVersion, setListVersion] = useState(0)
  const [conversationPage, setConversationPage] = useState<PagedResponse<Conversation> | null>(null)
  const [listLoading, setListLoading] = useState(true)
  const [listError, setListError] = useState<string | null>(null)
  const [newTitle, setNewTitle] = useState('')
  const [createError, setCreateError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    listConversations(listPage, CONVERSATION_PAGE_SIZE, controller.signal)
      .then((response) => {
        setConversationPage(response)
        setListError(null)
      })
      .catch((error: unknown) => {
        if (!isAbortError(error)) {
          setListError(getErrorMessage(error, 'Unable to load your conversations.'))
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setListLoading(false)
      })

    return () => controller.abort()
  }, [listPage, listVersion])

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const title = newTitle.trim()
    if (title === '') {
      setCreateError('Give the conversation a title.')
      return
    }

    setCreating(true)
    setCreateError(null)
    try {
      const created = await createConversation(title)
      setNewTitle('')
      setListLoading(true)
      setListError(null)
      setListPage(0)
      setListVersion((current) => current + 1)
      navigate(`/app/conversations/${created.id}`)
    } catch (error) {
      setCreateError(getErrorMessage(error, 'Unable to create the conversation.'))
    } finally {
      setCreating(false)
    }
  }

  function changeListPage(nextPage: number) {
    setListLoading(true)
    setListPage(nextPage)
    setListError(null)
  }

  return (
    <section className="conversation-workspace">
      <aside className="conversation-sidebar">
        <div className="conversation-sidebar-heading">
          <div>
            <p className="card-kicker">Your workspace</p>
            <h1>Conversations</h1>
          </div>
          <span>{conversationPage?.totalElements ?? 0}</span>
        </div>

        <form className="new-conversation-form" onSubmit={handleCreate} noValidate>
          <label htmlFor="conversation-title">Start a conversation</label>
          <div>
            <input
              id="conversation-title"
              maxLength={200}
              value={newTitle}
              placeholder="CloudDesk recovery"
              onChange={(event) => {
                setNewTitle(event.target.value)
                setCreateError(null)
              }}
            />
            <button type="submit" disabled={creating}>
              {creating ? 'Creating…' : 'Create'}
            </button>
          </div>
          {createError !== null && <p className="field-error" role="alert">{createError}</p>}
        </form>

        <div className="conversation-list" aria-label="Your conversations">
          {listLoading && <p className="conversation-list-state">Loading conversations…</p>}
          {!listLoading && listError !== null && (
            <div className="conversation-list-state">
              <p>{listError}</p>
              <button
                className="secondary-button"
                type="button"
                onClick={() => {
                  setListLoading(true)
                  setListError(null)
                  setListVersion((current) => current + 1)
                }}
              >
                Try again
              </button>
            </div>
          )}
          {!listLoading && listError === null && conversationPage?.content.length === 0 && (
            <p className="conversation-list-state">
              Create your first conversation to start asking grounded questions.
            </p>
          )}
          {!listLoading && listError === null && conversationPage?.content.map((conversation) => (
            <Link
              className={conversation.id === conversationId ? 'conversation-link active' : 'conversation-link'}
              key={conversation.id}
              to={`/app/conversations/${conversation.id}`}
            >
              <strong>{conversation.title}</strong>
              <small>Updated {formatDate(conversation.updatedAt)}</small>
            </Link>
          ))}
        </div>

        {conversationPage !== null && conversationPage.totalPages > 1 && (
          <nav className="conversation-pagination" aria-label="Conversation pages">
            <button
              type="button"
              disabled={conversationPage.first || listLoading}
              onClick={() => changeListPage(Math.max(0, listPage - 1))}
            >
              Previous
            </button>
            <span>{conversationPage.page + 1} / {conversationPage.totalPages}</span>
            <button
              type="button"
              disabled={conversationPage.last || listLoading}
              onClick={() => changeListPage(listPage + 1)}
            >
              Next
            </button>
          </nav>
        )}
      </aside>

      <div className="conversation-panel">
        {conversationId === undefined && (
          <div className="conversation-welcome">
            <span aria-hidden="true">ASK</span>
            <p className="eyebrow"><span aria-hidden="true" />Grounded company knowledge</p>
            <h2>Ask a question. Inspect the evidence.</h2>
            <p>
              Choose a conversation or create a new one. Each supported answer links back to
              the document and page retrieved from your knowledge base.
            </p>
          </div>
        )}

        {conversationId !== undefined && (
          <ConversationDetail
            key={conversationId}
            conversationId={conversationId}
            onConversationUpdated={() => setListVersion((current) => current + 1)}
          />
        )}
      </div>
    </section>
  )
}
