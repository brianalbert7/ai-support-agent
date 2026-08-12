import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

function SessionLoadingScreen() {
  return (
    <main className="session-loading" aria-live="polite">
      <span className="loading-mark" aria-hidden="true">AI</span>
      <p>Restoring your secure session…</p>
    </main>
  )
}

export function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'checking') {
    return <SessionLoadingScreen />
  }

  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}

export function PublicOnlyRoute() {
  const { status } = useAuth()

  if (status === 'checking') {
    return <SessionLoadingScreen />
  }

  if (status === 'authenticated') {
    return <Navigate to="/app" replace />
  }

  return <Outlet />
}

export function AdminRoute() {
  const { user } = useAuth()

  if (user?.role !== 'ADMIN') {
    return <Navigate to="/app/access-denied" replace />
  }

  return <Outlet />
}
