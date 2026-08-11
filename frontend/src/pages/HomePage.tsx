import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const workflowSteps = [
  {
    number: '01',
    title: 'Ingest trusted knowledge',
    description: 'Upload company PDFs and preserve the page-level source of every passage.',
  },
  {
    number: '02',
    title: 'Retrieve before answering',
    description: 'Find the most relevant chunks with vector similarity instead of guessing.',
  },
  {
    number: '03',
    title: 'Verify every response',
    description: 'Return grounded answers with citations employees can inspect and trust.',
  },
]

export default function HomePage() {
  const { status } = useAuth()

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="#top" aria-label="AI Support Agent home">
          <span className="brand-mark" aria-hidden="true">AI</span>
          <span>AI Support Agent</span>
        </a>
        <nav className="header-actions" aria-label="Primary navigation">
          <a className="header-link" href="#workflow">How it works</a>
          {status === 'authenticated' ? (
            <Link className="compact-action" to="/app">Open workspace</Link>
          ) : (
            <Link className="compact-action" to="/login">Sign in</Link>
          )}
        </nav>
      </header>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="eyebrow"><span aria-hidden="true" />Grounded by your documents</p>
          <h1>Company knowledge, with receipts.</h1>
          <p className="hero-summary">
            Ask natural-language questions across internal policies, manuals, and support
            documentation. Get concise answers backed by the exact source and page.
          </p>
          <div className="hero-actions">
            {status === 'authenticated' ? (
              <Link className="primary-action" to="/app">Open your workspace</Link>
            ) : (
              <Link className="primary-action" to="/register">Create an account</Link>
            )}
            <span className="availability"><span aria-hidden="true" />Cited answer preview</span>
          </div>
        </div>

        <div className="answer-preview" aria-label="Example grounded answer">
          <div className="preview-header">
            <div>
              <p className="preview-label">Knowledge assistant</p>
              <p className="preview-status"><span aria-hidden="true" />Grounded search</p>
            </div>
            <span className="preview-badge">1 verified source</span>
          </div>

          <div className="question-card">
            <span className="avatar user-avatar" aria-hidden="true">BA</span>
            <div>
              <p className="message-author">You</p>
              <p>How long does a customer password reset link remain valid?</p>
            </div>
          </div>

          <div className="answer-card">
            <span className="avatar assistant-avatar" aria-hidden="true">AI</span>
            <div>
              <p className="message-author">AI Support Agent</p>
              <p>
                Password reset links expire after <strong>30 minutes</strong>. If the link has
                expired, send a new one from the customer profile.
              </p>
              <div className="citation">
                <span className="document-icon" aria-hidden="true">PDF</span>
                <span>
                  <strong>Support Operations Manual</strong>
                  <small>Page 12 · Account security</small>
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="workflow" id="workflow" aria-labelledby="workflow-title">
        <div className="section-heading">
          <p className="eyebrow"><span aria-hidden="true" />Built for trustworthy answers</p>
          <h2 id="workflow-title">From document to defensible answer.</h2>
        </div>
        <div className="workflow-grid">
          {workflowSteps.map((step) => (
            <article className="workflow-card" key={step.number}>
              <span className="step-number">{step.number}</span>
              <h3>{step.title}</h3>
              <p>{step.description}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  )
}
