import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { apiRequest } from '../lib/apiClient'
import type {
  AuthenticationTokens,
  LoginCredentials,
  RegistrationDetails,
  UserProfile,
} from '../types/auth'
import {
  AUTH_SESSION_EXPIRED_EVENT,
  authenticatedApiRequest,
  clearSession,
  getRefreshToken,
  hasRefreshToken,
  refreshAccessToken,
  setSessionTokens,
} from './authSession'

type AuthenticationStatus = 'checking' | 'anonymous' | 'authenticated'

interface AuthContextValue {
  status: AuthenticationStatus
  user: UserProfile | null
  login: (credentials: LoginCredentials) => Promise<void>
  register: (details: RegistrationDetails) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

async function loadCurrentUser(): Promise<UserProfile> {
  return authenticatedApiRequest<UserProfile>('/users/me')
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthenticationStatus>('checking')
  const [user, setUser] = useState<UserProfile | null>(null)

  const completeAuthentication = useCallback(async (tokens: AuthenticationTokens) => {
    setSessionTokens(tokens)

    try {
      const profile = await loadCurrentUser()
      setUser(profile)
      setStatus('authenticated')
    } catch (error) {
      clearSession()
      setUser(null)
      setStatus('anonymous')
      throw error
    }
  }, [])

  const login = useCallback(
    async (credentials: LoginCredentials) => {
      const tokens = await apiRequest<AuthenticationTokens>('/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials),
      })
      await completeAuthentication(tokens)
    },
    [completeAuthentication],
  )

  const register = useCallback(
    async (details: RegistrationDetails) => {
      const tokens = await apiRequest<AuthenticationTokens>('/auth/register', {
        method: 'POST',
        body: JSON.stringify(details),
      })
      await completeAuthentication(tokens)
    },
    [completeAuthentication],
  )

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken()

    try {
      if (refreshToken !== null) {
        await apiRequest<void>('/auth/logout', {
          method: 'POST',
          body: JSON.stringify({ refreshToken }),
        })
      }
    } finally {
      clearSession()
      setUser(null)
      setStatus('anonymous')
    }
  }, [])

  useEffect(() => {
    let active = true

    const expireSession = () => {
      if (active) {
        setUser(null)
        setStatus('anonymous')
      }
    }

    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, expireSession)

    async function initializeSession() {
      if (!hasRefreshToken()) {
        setStatus('anonymous')
        return
      }

      try {
        await refreshAccessToken()
        const profile = await loadCurrentUser()
        if (active) {
          setUser(profile)
          setStatus('authenticated')
        }
      } catch {
        if (active) {
          clearSession()
          setUser(null)
          setStatus('anonymous')
        }
      }
    }

    void initializeSession()

    return () => {
      active = false
      window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, expireSession)
    }
  }, [])

  const value = useMemo(
    () => ({ status, user, login, register, logout }),
    [status, user, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}
