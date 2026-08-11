import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import AuthLayout from '../components/AuthLayout'
import { getErrorMessage, getFieldErrors } from '../lib/apiError'

interface LoginForm {
  email: string
  password: string
}

interface RedirectState {
  from?: { pathname?: string }
}

function validate(form: LoginForm): Record<string, string> {
  const errors: Record<string, string> = {}
  if (form.email.trim() === '') {
    errors.email = 'Email is required'
  }
  if (form.password === '') {
    errors.password = 'Password is required'
  }
  return errors
}

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState<LoginForm>({ email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationErrors = validate(form)
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors)
      setSubmitError(null)
      return
    }

    setSubmitting(true)
    setFieldErrors({})
    setSubmitError(null)

    try {
      await login({ email: form.email.trim(), password: form.password })
      const redirectState = location.state as RedirectState | null
      const destination = redirectState?.from?.pathname ?? '/app'
      const safeDestination = destination.startsWith('/') && !destination.startsWith('//')
        ? destination
        : '/app'
      navigate(safeDestination, { replace: true })
    } catch (error) {
      setFieldErrors(getFieldErrors(error))
      setSubmitError(getErrorMessage(error, 'Unable to sign in. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Secure sign in"
      title="Welcome back."
      summary="Use your company account to enter the knowledge workspace."
      footer={<p>New here? <Link to="/register">Create an employee account</Link></p>}
    >
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        {submitError !== null && <div className="form-alert" role="alert">{submitError}</div>}

        <div className="form-field">
          <label htmlFor="login-email">Email address</label>
          <input
            id="login-email"
            name="email"
            type="email"
            autoComplete="email"
            value={form.email}
            aria-invalid={fieldErrors.email !== undefined}
            aria-describedby={fieldErrors.email ? 'login-email-error' : undefined}
            onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
          />
          {fieldErrors.email && <span id="login-email-error" className="field-error">{fieldErrors.email}</span>}
        </div>

        <div className="form-field">
          <div className="label-row">
            <label htmlFor="login-password">Password</label>
            <span>8–72 characters</span>
          </div>
          <input
            id="login-password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={form.password}
            aria-invalid={fieldErrors.password !== undefined}
            aria-describedby={fieldErrors.password ? 'login-password-error' : undefined}
            onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
          />
          {fieldErrors.password && <span id="login-password-error" className="field-error">{fieldErrors.password}</span>}
        </div>

        <button className="submit-button" type="submit" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in securely'}
        </button>
      </form>
    </AuthLayout>
  )
}
