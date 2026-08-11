import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import AuthLayout from '../components/AuthLayout'
import { getErrorMessage, getFieldErrors } from '../lib/apiError'
import type { RegistrationDetails } from '../types/auth'

function validate(form: RegistrationDetails): Record<string, string> {
  const errors: Record<string, string> = {}
  if (form.firstName.trim() === '') errors.firstName = 'First name is required'
  if (form.lastName.trim() === '') errors.lastName = 'Last name is required'
  if (form.email.trim() === '') errors.email = 'Email is required'
  if (form.password.length < 8) errors.password = 'Password must contain at least 8 characters'
  return errors
}

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState<RegistrationDetails>({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
  })
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
      await register({
        ...form,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
      })
      navigate('/app', { replace: true })
    } catch (error) {
      setFieldErrors(getFieldErrors(error))
      setSubmitError(getErrorMessage(error, 'Unable to create your account. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Employee registration"
      title="Create your account."
      summary="Registration creates an employee account. Administrator access is provisioned separately."
      footer={<p>Already registered? <Link to="/login">Sign in</Link></p>}
    >
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        {submitError !== null && <div className="form-alert" role="alert">{submitError}</div>}

        <div className="name-fields">
          <div className="form-field">
            <label htmlFor="register-first-name">First name</label>
            <input
              id="register-first-name"
              name="firstName"
              autoComplete="given-name"
              maxLength={50}
              value={form.firstName}
              aria-invalid={fieldErrors.firstName !== undefined}
              aria-describedby={fieldErrors.firstName ? 'register-first-name-error' : undefined}
              onChange={(event) => setForm((current) => ({ ...current, firstName: event.target.value }))}
            />
            {fieldErrors.firstName && <span id="register-first-name-error" className="field-error">{fieldErrors.firstName}</span>}
          </div>

          <div className="form-field">
            <label htmlFor="register-last-name">Last name</label>
            <input
              id="register-last-name"
              name="lastName"
              autoComplete="family-name"
              maxLength={50}
              value={form.lastName}
              aria-invalid={fieldErrors.lastName !== undefined}
              aria-describedby={fieldErrors.lastName ? 'register-last-name-error' : undefined}
              onChange={(event) => setForm((current) => ({ ...current, lastName: event.target.value }))}
            />
            {fieldErrors.lastName && <span id="register-last-name-error" className="field-error">{fieldErrors.lastName}</span>}
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="register-email">Email address</label>
          <input
            id="register-email"
            name="email"
            type="email"
            autoComplete="email"
            maxLength={255}
            value={form.email}
            aria-invalid={fieldErrors.email !== undefined}
            aria-describedby={fieldErrors.email ? 'register-email-error' : undefined}
            onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
          />
          {fieldErrors.email && <span id="register-email-error" className="field-error">{fieldErrors.email}</span>}
        </div>

        <div className="form-field">
          <div className="label-row">
            <label htmlFor="register-password">Password</label>
            <span>8–72 characters</span>
          </div>
          <input
            id="register-password"
            name="password"
            type="password"
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            value={form.password}
            aria-invalid={fieldErrors.password !== undefined}
            aria-describedby={fieldErrors.password ? 'register-password-error' : undefined}
            onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
          />
          {fieldErrors.password && <span id="register-password-error" className="field-error">{fieldErrors.password}</span>}
        </div>

        <button className="submit-button" type="submit" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create employee account'}
        </button>
      </form>
    </AuthLayout>
  )
}
