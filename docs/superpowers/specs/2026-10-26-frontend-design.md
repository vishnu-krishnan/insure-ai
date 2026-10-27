# Frontend design

## Intent

Give internal agents a professional web UI to create customers and quotes against the existing Quote Service and Customer Service APIs, and give policyholders a customer-facing area in the same app. Today the project has no front end; every operation goes through Postman or Swagger UI.

## Goals

- Agents can create a customer, look up a customer by ID, create a quote for an existing customer (`POST /api/v2/quotes`), open a quote by ID, and browse the quote list (`GET /api/v1/quotes?page&size`).
- Login through the existing Keycloak realm using OIDC authorization code with PKCE. No client secret in the browser.
- Consistent, accessible, responsive layout with form validation that mirrors server rules and shows the server's Problem Details (`code`, `errors`, `correlationId`) when a call fails.
- One app, one deployment, role-based areas: `/agent/*` for agents and `/me/*` for customers.

## Non-goals

- No new backend capabilities in the first delivery (see Phasing).
- No mobile app, no i18n beyond English, no admin screens, no payments or policy issuance.
- No self-registration; accounts are created in Keycloak.

## Anti-goals

- Customer browsers must never download agent screens (route-level code splitting gated on role).
- No token or secret persisted in `localStorage`; no JWT validation disabled for development convenience.
- Hiding a screen is never treated as authorization; the services remain the enforcement point.

## Constraints

- Backend contract is fixed by the services: scopes `quote:create`, `quote:read`, `customer:create`, `customer:read`; token audience per service; CORS is not yet configured on either service.
- The Keycloak realm is not yet configured with the clients, scopes and audience mappers from `docs/keycloak.md`. Real login cannot be exercised until it is. Changing Keycloak state needs the user's approval.
- Ports in use locally: 5433, 8280, 8281, 8282. The dev server must use a different loopback port (proposed 3174).
- The repo must not show tooling traces: no generated-by banners, no co-author lines, no boilerplate readmes.
- Nothing is committed unless the user asks.

## Approaches considered

1. Two apps with a shared package: strongest separation, more setup.
2. **One app with role-based areas (chosen by the user).** Least code and one deployment. Mitigated by lazy-loaded, role-gated route chunks and by server-side enforcement.
3. Agent app first, customer portal later: this is the delivery order inside approach 2.

## Decisions

Decided with the user: agent UI for internal use, under `frontend/`; also a customer-facing area; one app (approach 2).

Proposed by me (my calls, open to change):

- React, TypeScript and Vite; React Router; TanStack Query for server state; React Hook Form with Zod for forms; `oidc-client-ts` (or `react-oidc-context`) for Keycloak login; a component library with good accessibility (Mantine or MUI) rather than hand-rolled components; Vitest and Testing Library for tests; Playwright for one end-to-end flow later.
- Tokens held in memory only; silent renew through the refresh flow; logout through Keycloak end-session.
- API base URLs from `VITE_*` variables documented in a `frontend/.env.example`; a Vite dev proxy to avoid CORS locally, and CORS configuration on the services is a separate backend change for deployed environments.
- Typed API client generated from each service's `/v3/api-docs` (already published, toggled by `API_DOCS_ENABLED`).
- Screens: sign-in redirect, agent dashboard, customer create form, customer lookup and detail, quote create form (select customer, then fields), quote detail, paginated quote list, customer area showing "my quotes" and "my profile", error and not-found pages.

Calls made while reviewing the design (mine, open to change):

- Roles come from the access token's `realm_access.roles` claim; realm roles are `agent` and `customer`. The route gate reads only that claim.
- One public Keycloak client (`insure-ui`) with PKCE, issuing one access token whose audience contains both the Quote and Customer service audiences through audience mappers. The API client sends that same token to both services.
- Customers have no list or search endpoint, so the quote form takes a customer ID with an inline lookup that shows the customer's name before submit. That is intended, not a gap.
- API contract source of truth is each service's `/v3/api-docs`. Known today: `POST /api/v1/quotes`, `POST /api/v2/quotes`, `GET /api/v1/quotes/{quoteId}`, and `GET /api/v1/quotes?page&size` returning `content`, `page`, `size`, `totalElements`, `totalPages`; `POST /api/v1/customers` and `GET /api/v1/customers/{customerId}`.
- npm with Node 22 LTS; the generated API client is committed and regenerated with an npm script against a running service, so fresh checkouts and CI need no backend.

Decided with the user, verification of phase 1: both. A dev-only mock sign-in in the front end (excluded from production builds, never touching backend JWT validation) backs unit and component tests; the local Keycloak realm is also configured for real login and one end-to-end check. The user approved configuring the local Keycloak realm for this (client `insure-ui`, realm roles `agent` and `customer`, scopes and audience mappers from `docs/keycloak.md`). That approval covers the local instance only; the exact changes will be listed in the plan before they are applied.

## Phasing

1. Frontend skeleton, auth, shared layout, API client, agent screens. Works against the current backend once the Keycloak realm is configured.
2. Backend change, as its own plan for approval: a `customerId` claim in customer tokens and per-customer authorization on the customer and quote read endpoints, so a customer can only see their own data.
3. Customer area screens on top of phase 2. Until phase 2 lands the `/me/*` routes exist but are disabled.

## Left to the builder

- Choice between Mantine and MUI, the folder layout inside `frontend/src`, exact form-validation messages, loading and empty states, and colour tokens, within the "professional, accessible, light and dark" brief.
- Wiring of CI for the frontend (lint, test, build) alongside the existing workflow.
- Dockerfile and compose entry for serving the built app, only if the user asks.

## Open points for the user

- Approval of the Keycloak realm configuration (clients `insure-agent-ui` and `insure-customer-ui` or one shared public client, roles `agent` and `customer`, and the scopes and audience mappers).
- Whether phase 2 (customer-level authorization in the backend) should be planned now or after the agent screens ship.
