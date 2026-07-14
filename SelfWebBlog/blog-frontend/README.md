# Gabriel Blog Frontend

Vue 3 + Vite frontend for SelfWebBlog. The application is an article-first personal blog with authoring, interaction, discovery, appearance, and administration workflows.

## Requirements

- Node.js 20+
- pnpm 10+
- SelfWebBlog backend on `http://localhost:8080`

## Local Development

```powershell
pnpm install --frozen-lockfile
pnpm dev --host 127.0.0.1 --port 5174
```

Vite proxies `/api`, `/uploads`, and `/actuator` to the local backend.

## Quality Checks

```powershell
pnpm test
pnpm lint
pnpm build
pnpm format:check
pnpm test:e2e
```

`pnpm check` runs unit tests, ESLint, and the production build. Playwright tests use desktop, tablet, and mobile viewports and reuse an existing server on port 5174.

## Routes

- `/`: article-first home
- `/archive`: URL-backed search and filters
- `/tags`, `/tags/:slug`: tag discovery
- `/calendar`: publication calendar
- `/post/:id`: reading, engagement, and sharing
- `/guestbook`: public guestbook
- `/about`: author profile
- `/login`: login and registration
- `/me`: visitor profile
- `/write`, `/write/:id`: authoring
- `/admin`: administration workbench

## Source Boundaries

```text
src/
  api/            HTTP client and resource APIs
  app/            router and application bootstrapping
  components/ui/  reusable presentation primitives
  features/
    admin/        administration workbench
    auth/         login and registration
    content/      posts, filters, tags, and calendar
    editor/       Markdown editor, uploads, and publishing
    interaction/  comments, replies, likes, and guestbook
    site/         author, statistics, and share cards
  pages/          route composition
  styles/         tokens, base, motion, and Markdown styles
```

Pages compose feature components. API access belongs in `src/api` or a feature API module. Do not create parallel theme controls, interaction cards, post feeds, or filter state.

## Authentication

The frontend uses an HttpOnly session cookie and a readable `XSRF-TOKEN` cookie. The shared Axios interceptor sends `X-XSRF-TOKEN` for writes. JWTs and OAuth secrets must never be stored in frontend code or localStorage.

## Production

```powershell
pnpm build
pnpm preview --host 127.0.0.1 --port 4174
```

Production hosting must provide Vue Router history fallback and proxy `/api`, `/uploads`, and `/actuator/health` to the backend. Deployment and environment requirements are maintained in the local project documentation directory.
