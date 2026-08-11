# AI Support Agent frontend

React and TypeScript client for the AI Support Agent backend. This first phase provides the application shell, routing, API boundary, responsive styling, and frontend test setup. Authentication and live product workflows are implemented in later phases.

## Local development

The Spring Boot API must be available at `http://localhost:8080`.

```bash
nvm use
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to Spring Boot, keeping the browser client independent from the backend address during local development.

## Commands

```bash
npm run dev
npm run test
npm run lint
npm run build
```

Copy `.env.example` to `.env` only when the default API locations need to be changed.
