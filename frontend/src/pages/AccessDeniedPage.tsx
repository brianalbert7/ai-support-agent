import { Link } from 'react-router-dom'

export default function AccessDeniedPage() {
  return (
    <section className="workspace-main access-denied-page">
      <p className="eyebrow"><span aria-hidden="true" />Employee access</p>
      <h1>This area is for administrators.</h1>
      <p>
        Your account is authenticated, but document ingestion changes the shared company
        knowledge base and therefore requires the ADMIN role.
      </p>
      <Link className="primary-action" to="/app">Return to your workspace</Link>
    </section>
  )
}
