import { Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { AdminRoute, ProtectedRoute, PublicOnlyRoute } from './auth/RouteGuards'
import WorkspaceLayout from './components/WorkspaceLayout'
import AccessDeniedPage from './pages/AccessDeniedPage'
import ConversationsPage from './pages/ConversationsPage'
import DashboardPage from './pages/DashboardPage'
import DocumentsPage from './pages/DocumentsPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import RegisterPage from './pages/RegisterPage'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<HomePage />} />

        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route element={<WorkspaceLayout />}>
            <Route path="/app" element={<DashboardPage />} />
            <Route path="/app/access-denied" element={<AccessDeniedPage />} />
            <Route path="/app/conversations" element={<ConversationsPage />} />
            <Route path="/app/conversations/:conversationId" element={<ConversationsPage />} />
            <Route element={<AdminRoute />}>
              <Route path="/app/documents" element={<DocumentsPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AuthProvider>
  )
}
