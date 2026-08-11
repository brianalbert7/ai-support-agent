# AI Support Agent frontend

React and TypeScript client for the AI Support Agent backend. It provides a responsive product shell, typed API boundary, JWT authentication, protected routing, and frontend tests. Document administration and grounded conversation screens are implemented in later phases.

## Authentication design

- Login and registration use the Spring endpoints under `/api/auth`.
- Access tokens remain in memory and are attached to protected requests as Bearer tokens.
- The refresh token is kept in `sessionStorage`, rotated on page restoration, and removed when the browser tab closes.
- A protected request that receives `401 Unauthorized` performs one shared refresh operation and retries once.
- Logout revokes the refresh token through the backend and always clears local credentials.
- `/app` is protected, while `/login` and `/register` redirect authenticated users back to the workspace.

`sessionStorage` is a deliberate compromise for the current token-in-response backend contract. It is still readable by JavaScript if an XSS vulnerability exists. A stronger production design would use a `Secure`, `HttpOnly`, `SameSite` refresh-token cookie with an appropriate CSRF strategy.

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

## Commands

```bash
npm run dev
npm run test
npm run lint
npm run build
```

Copy `.env.example` to `.env` only when the default API locations need to be changed.
