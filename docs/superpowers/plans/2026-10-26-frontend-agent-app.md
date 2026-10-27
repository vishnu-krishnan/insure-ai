# Frontend Agent App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build phase 1 of the frontend: a single React app in `frontend/` where signed-in agents create and look up customers and create, open and list quotes against the existing services.

**Architecture:** One Vite app. `src/api` holds a typed HTTP client, Zod schemas and per-service functions; `src/auth` holds the Keycloak OIDC provider, a dev-only mock provider and the role gate; `src/features/{customers,quotes}` hold screens; `src/app` holds the shell and routes. Agent routes are lazy chunks behind a role gate. The `/me/*` area exists as a disabled placeholder until the backend phase 2 (a separate plan).

**Tech Stack:** Node 22 LTS, npm, React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, `oidc-client-ts`, Mantine, Vitest, Testing Library, MSW, Playwright.

**Spec:** `docs/superpowers/specs/2026-10-26-frontend-design.md`

## Global Constraints

- App lives in `frontend/`; dev server on loopback port `3174` (`server.host` 127.0.0.1).
- Auth is OIDC authorization code with PKCE and a public client `insure-ui`; no client secret; tokens in memory only, never `localStorage` or `sessionStorage`.
- Roles are read only from the access token claim `realm_access.roles`; realm roles are `agent` and `customer`.
- One access token is sent to both services; scopes used: `quote:create`, `quote:read`, `customer:create`, `customer:read`.
- Backend JWT validation is never weakened. The mock sign-in exists only when `import.meta.env.DEV` and `VITE_AUTH_MODE=mock`, and is absent from production builds (a test asserts this).
- API base paths: customers `/api/v1/customers` (proxied to `http://localhost:8282`), quotes `/api/v1/quotes` and `/api/v2/quotes` (proxied to `http://localhost:8281`). Ports and URLs come from `VITE_*` variables listed in `frontend/.env.example`.
- Money values are decimal strings or `Decimal`-safe, never binary floats.
- Errors from the services are RFC 9457 Problem Details with `code`, `correlationId` and an `errors` list of `{field, message}`; the UI shows `correlationId` on unexpected failures.
- No tooling traces in the repo: no generated-by banners, co-author lines, emojis or boilerplate readmes. No commits unless the user asks; steps stage with `git add` only.
- Keycloak changes apply to the local instance only (port 8280) and are listed to the user before running.

## Review Focus

- Access token expires while a form is open: the call fails with 401, the user is redirected to sign in, and form input is not lost silently.
- Agent without the `agent` role opens `/agent/*`: sees an access-denied page, not a blank screen or a crash.
- Server field errors (`errors` list) appear next to the right inputs; a duplicate-looking submit is blocked while a request is in flight.
- Quote create for a customer ID that does not exist (`CUSTOMER_REFERENCE_INVALID` or `CUSTOMER_NOT_FOUND`) and for Customer Service being down (`CUSTOMER_SERVICE_UNAVAILABLE`, `CUSTOMER_SERVICE_TIMEOUT`) shows a clear message, not a generic failure.
- Quote list: `page` beyond the last page, empty list, and `size` outside 1-100 behave sensibly.
- Coverage amount with many digits or decimals (for example `1234567890.12`) round-trips without loss.

---

### Task 1: Scaffold, config and tooling

**Files:**
- Create: `frontend/package.json`, `frontend/tsconfig.json`, `frontend/vite.config.ts`, `frontend/index.html`, `frontend/.env.example`, `frontend/src/main.tsx`, `frontend/src/config.ts`, `frontend/src/test/setup.ts`, `frontend/eslint.config.js`
- Modify: `.gitignore` (add `node_modules/`, `frontend/dist/`), `.editorconfig` (2-space indent for `*.{ts,tsx,css}` if not already covered)
- Test: `frontend/src/config.test.ts`

**Interfaces:**
- Produces: `config: { authMode: 'oidc' | 'mock'; oidc: { authority: string; clientId: string; redirectUri: string; postLogoutRedirectUri: string }; apiBase: { customers: string; quotes: string } }` exported from `src/config.ts`; `loadConfig(env: Record<string, string | undefined>): Config` (pure, throws `Error` naming the missing variable).

- [ ] **Step 1: Write the failing test** `config.test.ts`: `loadConfig({})` throws an error containing `VITE_OIDC_AUTHORITY`; with `VITE_AUTH_MODE='mock'` it needs no OIDC variables; `VITE_AUTH_MODE='bogus'` throws.
- [ ] **Step 2: Run** `npm test -- config` in `frontend/`; expected FAIL (module missing).
- [ ] **Step 3: Scaffold** with `npm create vite@latest` (react-ts template) into `frontend/`, install the stack above, set `engines.node` to `>=22`, add scripts `dev`, `build`, `lint`, `test`, `e2e`; configure Vite dev proxy for the three API prefixes and `server.port 3174`; implement `loadConfig`; `.env.example` lists `VITE_AUTH_MODE`, `VITE_OIDC_AUTHORITY`, `VITE_OIDC_CLIENT_ID`, `VITE_CUSTOMER_API`, `VITE_QUOTE_API` with local defaults and no secrets.
- [ ] **Step 4: Run** `npm test && npm run lint && npm run build`; expected all pass.
- [ ] **Step 5: Stage** `git add frontend .gitignore .editorconfig`.

### Task 2: API client, schemas and service functions

**Files:**
- Create: `frontend/src/api/http.ts`, `frontend/src/api/problem.ts`, `frontend/src/api/schemas.ts`, `frontend/src/api/customers.ts`, `frontend/src/api/quotes.ts`
- Test: `frontend/src/api/http.test.ts`, `frontend/src/api/customers.test.ts`, `frontend/src/api/quotes.test.ts` (MSW handlers)

**Interfaces:**
- Consumes: `config.apiBase`.
- Produces:
  - `class ApiError extends Error { status: number; code: string; errors: {field: string; message: string}[]; correlationId?: string }` in `problem.ts`.
  - `setTokenProvider(fn: () => Promise<string | null>)` and `request<T>(path: string, init?: RequestInit): Promise<T>` in `http.ts` (adds `Authorization: Bearer`, parses Problem Details into `ApiError`, calls `onUnauthorized()` listener on 401).
  - Zod schemas `customerRequestSchema` (fullName 2-80; email valid, max 254; phone `^\+[1-9][0-9]{7,14}$`; dateOfBirth past date), `customerResponseSchema`, `customerQuoteRequestSchema` (customerId UUID; coverageType one of `AUTO|HOME|LIFE|HEALTH`; coverageAmount decimal string greater than 0), `customerQuoteResponseSchema` (`quoteId, customerId, customerName, coverageType, age, coverageAmount, annualPremium, monthlyPremium`), `quotePageSchema` (`content[{quoteId, customerName, coverageType, age, coverageAmount, annualPremium, monthlyPremium}], page, size, totalElements, totalPages`).
  - `createCustomer(input)`, `getCustomer(id)`, `createQuote(input)` (POST `/api/v2/quotes`), `getQuote(id)`, `listQuotes(page: number, size: number)`.

- [ ] **Step 1: Write failing tests**: 401 invokes the unauthorized listener; a 400 Problem Details body becomes `ApiError` with `code 'VALIDATION_FAILED'` and two field errors; a 404 on `getQuote` has `code 'QUOTE_NOT_FOUND'`; `listQuotes(0, 20)` requests `?page=0&size=20`; the schema rejects `coverageAmount '0'` and accepts `'1234567890.12'` unchanged as a string.
- [ ] **Step 2: Run** `npm test -- api`; expected FAIL.
- [ ] **Step 3: Implement** the signatures above; parse every response through its Zod schema.
- [ ] **Step 4: Run** `npm test -- api`; expected PASS.
- [ ] **Step 5: Stage** `git add frontend/src/api`.

### Task 3: Auth providers and role gate

**Files:**
- Create: `frontend/src/auth/AuthProvider.tsx`, `frontend/src/auth/mockAuth.ts`, `frontend/src/auth/roles.ts`, `frontend/src/auth/RequireRole.tsx`, `frontend/src/auth/useAuth.ts`
- Test: `frontend/src/auth/roles.test.ts`, `frontend/src/auth/RequireRole.test.tsx`, `frontend/src/auth/mockAuth.test.ts`

**Interfaces:**
- Consumes: `config`, `setTokenProvider`.
- Produces:
  - `rolesFromToken(accessToken: string): string[]` (decodes the payload only to read `realm_access.roles`; returns `[]` on malformed input; never validates, the services do).
  - `useAuth(): { status: 'loading' | 'authenticated' | 'anonymous'; user?: { name: string; roles: string[] }; signIn(): Promise<void>; signOut(): Promise<void>; getAccessToken(): Promise<string | null> }`.
  - `<RequireRole role="agent">` renders children, a sign-in redirect when anonymous, or an access-denied page when the role is missing.
  - `isMockAuthEnabled(): boolean` true only when `import.meta.env.DEV` and `config.authMode === 'mock'`.

- [ ] **Step 1: Write failing tests**: `rolesFromToken` returns `['agent']` for a token with that claim and `[]` for garbage; `RequireRole` shows an access-denied message for a user with only `customer`; mock auth yields a user with role `agent`; a test builds the app with `import.meta.env.DEV=false` and asserts `isMockAuthEnabled()` is false even when `VITE_AUTH_MODE=mock`.
- [ ] **Step 2: Run** `npm test -- auth`; expected FAIL.
- [ ] **Step 3: Implement** the OIDC provider with `oidc-client-ts` `UserManager` (`response_type code`, PKCE, `InMemoryWebStorage` for user and state stores, silent renew via refresh token, redirect callback route `/auth/callback`); the mock provider supplies a fixed fake token and is imported only through a dynamic import guarded by `import.meta.env.DEV`, so production bundles drop it.
- [ ] **Step 4: Run** `npm test -- auth && npm run build`; then confirm `grep -r "mockAuth" frontend/dist` finds nothing.
- [ ] **Step 5: Stage** `git add frontend/src/auth`.

### Task 4: App shell, routing and error pages

**Files:**
- Create: `frontend/src/app/App.tsx`, `frontend/src/app/routes.tsx`, `frontend/src/app/Shell.tsx`, `frontend/src/app/pages/{Dashboard,Forbidden,NotFound,CustomerAreaPlaceholder,ErrorFallback}.tsx`, `frontend/src/app/theme.ts`
- Modify: `frontend/src/main.tsx`
- Test: `frontend/src/app/routes.test.tsx`

**Interfaces:**
- Consumes: `RequireRole`, `useAuth`, feature route modules `customerRoutes`, `quoteRoutes` (Tasks 5 and 6, loaded with `React.lazy`).
- Produces: routes `/` (redirect by role), `/agent` (dashboard), `/agent/customers/*`, `/agent/quotes/*`, `/me/*` (disabled placeholder explaining the customer area is not yet available), `/auth/callback`, `*` (NotFound). Shell: top bar with app name, user name, sign-out; side navigation with Dashboard, Customers, Quotes; light and dark theme through Mantine's color-scheme toggle; responsive down to phone width.

- [ ] **Step 1: Write failing tests**: an anonymous visit to `/agent` starts sign-in; an `agent` user sees the dashboard; a `customer`-only user at `/agent` sees Forbidden; `/me` shows the placeholder; an unknown path shows NotFound; a thrown render error shows `ErrorFallback` and not a blank page.
- [ ] **Step 2: Run** `npm test -- routes`; expected FAIL.
- [ ] **Step 3: Implement** with React Router data routes and one `ErrorBoundary`; QueryClient defaults: no retry on 4xx.
- [ ] **Step 4: Run** `npm test && npm run build`; confirm agent chunks are separate files in `dist/assets`.
- [ ] **Step 5: Stage** `git add frontend/src/app frontend/src/main.tsx`.

### Task 5: Customer screens

**Files:**
- Create: `frontend/src/features/customers/{CustomerCreatePage,CustomerLookupPage,CustomerDetail,routes}.tsx`, `frontend/src/features/shared/{FormErrors.tsx,useApiForm.ts}`
- Test: `frontend/src/features/customers/*.test.tsx`

**Interfaces:**
- Consumes: `createCustomer`, `getCustomer`, `customerRequestSchema`, `ApiError`.
- Produces: `useApiForm(schema, submit)` wiring React Hook Form + Zod, mapping `ApiError.errors` onto fields and exposing `isSubmitting`; `customerRoutes` for `/agent/customers/new` and `/agent/customers/:customerId` (lookup box on `/agent/customers`).

- [ ] **Step 1: Write failing tests**: invalid phone shows the validation message before any request; a server `VALIDATION_FAILED` for `email` appears under the email input; the submit button is disabled while the request is pending and only one request is sent on double click; success navigates to the new customer's detail page; looking up an unknown ID shows `CUSTOMER_NOT_FOUND` as "Customer not found".
- [ ] **Step 2: Run** `npm test -- customers`; expected FAIL.
- [ ] **Step 3: Implement** the pages with Mantine form components, accessible labels and error text tied to inputs.
- [ ] **Step 4: Run** `npm test -- customers`; expected PASS.
- [ ] **Step 5: Stage** `git add frontend/src/features`.

### Task 6: Quote screens

**Files:**
- Create: `frontend/src/features/quotes/{QuoteCreatePage,QuoteDetailPage,QuoteListPage,CustomerPicker,routes}.tsx`
- Test: `frontend/src/features/quotes/*.test.tsx`

**Interfaces:**
- Consumes: `createQuote`, `getQuote`, `listQuotes`, `getCustomer`, `useApiForm`.
- Produces: `CustomerPicker` (UUID input plus a Look up button; shows the customer's name and email once found and reports the chosen `customerId` upward); `quoteRoutes` for `/agent/quotes` (paginated list, 20 per page, page size selector 10/20/50/100), `/agent/quotes/new`, `/agent/quotes/:quoteId`.

- [ ] **Step 1: Write failing tests**: submit is blocked until a customer is looked up; `CUSTOMER_REFERENCE_INVALID` shows "That customer does not exist"; `CUSTOMER_SERVICE_UNAVAILABLE` and `CUSTOMER_SERVICE_TIMEOUT` show a retry message; `1234567890.12` is sent as exactly that string; the list renders `totalElements`, disables Next on the last page, and an empty list shows an empty state; opening an unknown quote ID shows "Quote not found"; premiums are shown with two decimals.
- [ ] **Step 2: Run** `npm test -- quotes`; expected FAIL.
- [ ] **Step 3: Implement** with TanStack Query (`keepPreviousData` for pagination) and invalidate the list query after create.
- [ ] **Step 4: Run** `npm test`; expected PASS.
- [ ] **Step 5: Stage** `git add frontend/src/features/quotes`.

### Task 7: Local Keycloak realm configuration

**Files:**
- Create: `docker/keycloak/configure-realm.sh` (idempotent, uses the Keycloak admin REST API through `curl`; reads `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD` from the environment, never from arguments)
- Modify: `docs/keycloak.md` (add the UI client, roles and mapper sections), `.env.example` (UI client id and redirect values)

**Interfaces:**
- Consumes: the running `insure-keycloak` container on `127.0.0.1:8280`.
- Produces in the `insure-ai` realm: public client `insure-ui` (standard flow, PKCE S256, redirect URI `http://localhost:3174/auth/callback`, post-logout `http://localhost:3174/`, web origin `http://localhost:3174`); realm roles `agent` and `customer`; client scopes `quote:create`, `quote:read`, `customer:create`, `customer:read` attached to `insure-ui`; audience mappers adding the Quote and Customer audiences from `QUOTE_JWT_AUDIENCE` and `CUSTOMER_JWT_AUDIENCE`; a local test user `agent1` with role `agent` and a temporary password the script prints once to the terminal, never into a file.

- [ ] **Step 1: Show the user** the exact list above and the realm name read from `KEYCLOAK_ISSUER_URI`, and wait for a go-ahead even though the design approved this scope.
- [ ] **Step 2: Write the script** so that running it twice changes nothing the second time (check before create).
- [ ] **Step 3: Run it** and verify with a token request for `agent1` through the `insure-ui` client: decode the access token and confirm `aud` contains both audiences, `scope` contains the four scopes, and `realm_access.roles` contains `agent`.
- [ ] **Step 4: Verify the services accept it**: call `GET /api/v1/quotes?page=0&size=1` on 8281 and `GET /api/v1/customers/<unknown uuid>` on 8282 with that token; expected 200 and 404 respectively, not 401 or 403. (Services must be running on the Boot 4 jars; restart them first with the user's approval.)
- [ ] **Step 5: Stage** `git add docker/keycloak docs/keycloak.md .env.example`.

### Task 8: End-to-end check, CI and docs

**Files:**
- Create: `frontend/playwright.config.ts`, `frontend/e2e/agent-flow.spec.ts`
- Modify: `.github/workflows/ci.yml` (add a `frontend` job: Node 22, `npm ci`, `lint`, `test`, `build`; the e2e job is not run in CI), `README.md` (a Frontend section: install, run on 3174, mock mode, real realm, scripts)

**Interfaces:**
- Consumes: the real realm from Task 7 and both services running.
- Produces: one Playwright test signing in as `agent1` (password from the environment variable `E2E_AGENT_PASSWORD`), creating a customer, creating a quote for that customer, finding it in the list.

- [ ] **Step 1: Write the e2e spec** following the flow above, asserting the premium shown is two decimals and the quote appears on the first list page.
- [ ] **Step 2: Run** `npm run e2e` with both services, Postgres and Keycloak up; expected PASS. Report anything that could not run.
- [ ] **Step 3: Update CI and README**; verify the CI file parses and `npm ci && npm run lint && npm test && npm run build` pass from a clean `node_modules`.
- [ ] **Step 4: Stage** `git add frontend .github README.md`.

---

## Out of scope for this plan

Backend phase 2 (a `customerId` claim and per-customer authorization) and the `/me/*` customer screens get their own plan after this one ships. CORS configuration on the services for deployed environments is also separate; local development uses the Vite proxy.
