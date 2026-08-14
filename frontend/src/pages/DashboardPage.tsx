import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function DashboardPage() {
  const { user } = useAuth()

  if (user === null) {
    return null
  }

  return (
    <section className="workspace-main">
        <div className="workspace-intro">
          <p className="eyebrow"><span aria-hidden="true" />Authenticated workspace</p>
          <h1>Your knowledge workspace.</h1>
          <p>
            You are securely connected as <strong>{user.email}</strong>. Ask questions against
            the shared knowledge base and inspect the evidence behind each answer.
          </p>
        </div>

        <div className="workspace-grid">
          {user.role === 'ADMIN' ? (
            <Link className="workspace-card featured-card card-link" to="/app/documents">
              <span className="card-index">01</span>
              <div>
                <p className="card-kicker">Knowledge base</p>
                <h2>Turn company PDFs into searchable evidence.</h2>
                <p>Upload, process, and monitor documents through the admin API.</p>
              </div>
              <span className="coming-soon">Manage documents</span>
            </Link>
          ) : (
            <article className="workspace-card featured-card">
              <span className="card-index">01</span>
              <div>
                <p className="card-kicker">Knowledge base</p>
                <h2>Your company knowledge is managed by administrators.</h2>
                <p>Ready documents will become available when you start a conversation.</p>
              </div>
              <span className="coming-soon">Employee access</span>
            </article>
          )}

          <Link className="workspace-card card-link" to="/app/conversations">
            <span className="card-index">02</span>
            <div>
              <p className="card-kicker">Conversations</p>
              <h2>Ask questions that stay grounded.</h2>
              <p>Continue user-owned conversations and inspect their persisted citations.</p>
            </div>
            <span className="coming-soon">Start asking</span>
          </Link>
        </div>
    </section>
  )
}
