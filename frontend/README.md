# AI Support Agent frontend

React and TypeScript client for the AI Support Agent backend. It provides a responsive product shell, typed API boundary, JWT authentication, protected routing, administrator document management, grounded conversations with citations, and frontend tests.

## Authentication design

- Login and registration use the Spring endpoints under `/api/auth`.
- Access tokens remain in memory and are attached to protected requests as Bearer tokens.
- The refresh token is kept in `sessionStorage`, rotated on page restoration, and removed when the browser tab closes.
- A protected request that receives `401 Unauthorized` performs one shared refresh operation and retries once.
- Logout revokes the refresh token through the backend and always clears local credentials.
- `/app` is protected, while `/login` and `/register` redirect authenticated users back to the workspace.

`sessionStorage` is a deliberate compromise for the current token-in-response backend contract. It is still readable by JavaScript if an XSS vulnerability exists. A stronger production design would use a `Secure`, `HttpOnly`, `SameSite` refresh-token cookie with an appropriate CSRF strategy.

## Document management design

- Only administrators see the document navigation and can enter `/app/documents`.
- Spring service-layer authorization remains authoritative even if someone manually enters the URL.
- Uploads use multipart form data and retain the backend's 10 MB PDF validation.
- Upload and processing remain separate operations so `FAILED` documents can display their reason and be retried.
- The paginated list reflects the backend lifecycle states: `UPLOADED`, `PROCESSING`, `READY`, and `FAILED`.
- PostgreSQL and backend file storage remain the source of truth; documents are never persisted in browser storage.

## Conversation design

- Every authenticated user can create and reopen their own conversations under `/app/conversations`.
- Conversation and message history are loaded from the Spring API; the browser does not treat local state as durable storage.
- Sending a question appends the user/assistant pair returned by the backend, so the UI reflects exactly what was persisted.
- Grounded answers show expandable citation cards with the source document, page number, excerpt, and retrieval similarity.
- Unsupported answers are presented as knowledge gaps rather than being visually implied to have evidence.
- The first 100 chronological messages are loaded for this demo. A production chat with very long histories should add cursor-based history loading.

## Local development

The Spring Boot API must be available at `http://localhost:8080`.

```bash
nvm use
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to Spring Boot, keeping the browser client independent from the backend address during local development.

Available routes:

| Route | Purpose |
| --- | --- |
| `/` | Public product introduction |
| `/login` | User login |
| `/register` | Employee registration |
| `/app` | Authenticated workspace |
| `/app/conversations` | Owned conversation list and creation |
| `/app/conversations/:conversationId` | Persisted chat history, questions, and citations |
| `/app/documents` | Administrator document management |

## Commands

```bash
npm run dev
npm run test
npm run lint
npm run build
```

Copy `.env.example` to `.env` only when the default API locations need to be changed.
