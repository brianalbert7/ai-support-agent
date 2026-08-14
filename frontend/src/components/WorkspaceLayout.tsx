import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function WorkspaceLayout() {
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
      // AuthContext still clears local credentials when server revocation is unavailable.
    }
  }

  return (
    <main className="workspace-shell">
      <header className="workspace-header">
        <Link className="brand" to="/" aria-label="AI Support Agent home">
          <span className="brand-mark" aria-hidden="true">AI</span>
          <span>AI Support Agent</span>
        </Link>

        <nav className="workspace-nav" aria-label="Workspace navigation">
          <NavLink to="/app" end>Overview</NavLink>
          <NavLink to="/app/conversations">Conversations</NavLink>
          {user.role === 'ADMIN' && <NavLink to="/app/documents">Documents</NavLink>}
        </nav>

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

      <Outlet />
    </main>
  )
}
