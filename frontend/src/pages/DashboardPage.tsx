import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function DashboardPage() {
  const { user, logout } = useAuth()
  const [loggingOut, setLoggingOut] = useState(false)

  if (user === null) {
    return null
  }

  async function handleLogout() {
    setLoggingOut(true)
    try {
      await logout()
    } catch {
      // Local credentials are cleared in AuthContext even if server revocation is unavailable.
    }
  }

  return (
    <main className="workspace-shell">
      <header className="workspace-header">
        <Link className="brand" to="/" aria-label="AI Support Agent home">
          <span className="brand-mark" aria-hidden="true">AI</span>
          <span>AI Support Agent</span>
        </Link>
        <div className="account-menu">
          <span className="account-avatar" aria-hidden="true">
            {user.firstName.charAt(0)}{user.lastName.charAt(0)}
          </span>
          <span className="account-copy">
            <strong>{user.firstName} {user.lastName}</strong>
            <small>{user.role}</small>
          </span>
          <button className="text-button" type="button" onClick={handleLogout} disabled={loggingOut}>
            {loggingOut ? 'Signing out…' : 'Sign out'}
          </button>
        </div>
      </header>

      <section className="workspace-main">
        <div className="workspace-intro">
          <p className="eyebrow"><span aria-hidden="true" />Authenticated workspace</p>
          <h1>Your knowledge workspace.</h1>
          <p>
            You are securely connected as <strong>{user.email}</strong>. Document management
            and grounded conversations will be added in the next focused frontend phases.
          </p>
        </div>

        <div className="workspace-grid">
          <article className="workspace-card featured-card">
            <span className="card-index">01</span>
            <div>
              <p className="card-kicker">Knowledge base</p>
              <h2>Turn company PDFs into searchable evidence.</h2>
              <p>Upload, process, and monitor documents through the existing admin API.</p>
            </div>
            <span className="coming-soon">Next phase</span>
          </article>

          <article className="workspace-card">
            <span className="card-index">02</span>
            <div>
              <p className="card-kicker">Conversations</p>
              <h2>Ask questions that stay grounded.</h2>
              <p>Continue user-owned conversations and inspect their persisted citations.</p>
            </div>
            <span className="coming-soon">Planned</span>
          </article>
        </div>
      </section>
    </main>
  )
}
