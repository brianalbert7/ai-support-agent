import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

interface AuthLayoutProps {
  eyebrow: string
  title: string
  summary: string
  children: ReactNode
  footer: ReactNode
}

export default function AuthLayout({
  eyebrow,
  title,
  summary,
  children,
  footer,
}: AuthLayoutProps) {
  return (
    <main className="auth-shell">
      <section className="auth-story" aria-label="Product introduction">
        <Link className="brand auth-brand" to="/" aria-label="AI Support Agent home">
          <span className="brand-mark" aria-hidden="true">AI</span>
          <span>AI Support Agent</span>
        </Link>

        <div className="auth-story-copy">
          <p className="eyebrow light-eyebrow"><span aria-hidden="true" />Verifiable by design</p>
          <h2>Answers are useful.<br />Evidence makes them trustworthy.</h2>
          <p>
            Search internal knowledge, preserve conversation context, and verify every answer
            against its original document and page.
          </p>
        </div>

        <ul className="trust-list" aria-label="Security and trust features">
          <li><span aria-hidden="true">01</span>JWT authentication</li>
          <li><span aria-hidden="true">02</span>Role-based access</li>
          <li><span aria-hidden="true">03</span>Grounded citations</li>
        </ul>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-wrapper">
          <p className="eyebrow"><span aria-hidden="true" />{eyebrow}</p>
          <h1>{title}</h1>
          <p className="auth-summary">{summary}</p>
          {children}
          <div className="auth-footer">{footer}</div>
        </div>
      </section>
    </main>
  )
}
