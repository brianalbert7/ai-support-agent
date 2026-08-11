export type UserRole = 'EMPLOYEE' | 'ADMIN'

export interface AuthenticationTokens {
  accessToken: string
  refreshToken: string
}

export interface LoginCredentials {
  email: string
  password: string
}

export interface RegistrationDetails extends LoginCredentials {
  firstName: string
  lastName: string
}

export interface UserProfile {
  id: string
  firstName: string
  lastName: string
  email: string
  role: UserRole
}
