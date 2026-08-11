import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <main className="not-found">
      <p className="eyebrow"><span aria-hidden="true" />404</p>
      <h1>That page is not in the knowledge base.</h1>
      <p>The address may be incorrect, or the page may have moved.</p>
      <Link className="primary-action" to="/">Return home</Link>
    </main>
  )
}
