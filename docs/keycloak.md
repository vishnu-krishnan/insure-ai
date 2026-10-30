# Keycloak authentication and local development

Both Quote Service and Customer Service use Spring Security OAuth2 Resource Server. Spring's Nimbus decoder obtains signing keys from the configured Keycloak issuer metadata and validates signatures, issuer, expiration, and not-before timestamps. Each service additionally requires its configured JWT audience. No application code signs or manually parses tokens.

## Required environment

Configure these values in the ignored repository-root `.env` file, using `.env.example` as the template:

| Variable | Use |
|---|---|
| `KEYCLOAK_PUBLIC_URL` | Public base URL that Keycloak advertises. |
| `KEYCLOAK_ISSUER_URI` | Exact issuer URI for the selected realm, copied from that realm's OpenID configuration. |
| `KEYCLOAK_TOKEN_URI` | Realm's OAuth2 token endpoint, copied from the same OpenID configuration; used by Quote Service's client-credentials flow and by Postman. |
| `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD` | Keycloak administrator used by Compose to bootstrap Keycloak and by `docker/keycloak/configure-realm.sh`. After first start, create a permanent admin in the `master` realm, delete the temporary one, and put the permanent account here. |
| `QUOTE_JWT_AUDIENCE` | Audience Quote Service accepts; configure the matching Keycloak audience mapper. |
| `CUSTOMER_JWT_AUDIENCE` | Audience Customer Service accepts; configure the matching Keycloak audience mapper. |
| `KEYCLOAK_QUOTE_CLIENT_ID`, `KEYCLOAK_QUOTE_CLIENT_SECRET` | Credentials of the `insure-backend` client (see [Backend client](#backend-client)). Quote Service uses them for its Customer Service client-credentials flow, requesting only `customer:read`. |

Database credentials in the same file are required by Compose. Do not commit `.env`, client secrets, tokens, signing keys, or production URLs.

## Realm and clients

Create a realm in the local Keycloak Admin Console. The realm name is deliberately not fixed by the application; set `KEYCLOAK_ISSUER_URI` to the exact `issuer` value and `KEYCLOAK_TOKEN_URI` to `token_endpoint` from:

```text
<KEYCLOAK_PUBLIC_URL>/realms/<realm>/.well-known/openid-configuration
```

Configure the realm and its clients as follows:

1. Create the confidential `insure-backend` client with service accounts enabled (see [Backend client](#backend-client)). Set its client ID and secret in the environment.
2. For browser users, use the public `insure-ui` client (see [Web UI client](#web-ui-client)). For server-side callers such as Postman, use `insure-backend`. Access tokens must have an `aud` containing the configured Quote Service audience and a `scope` claim granting `quote:create` and/or `quote:read`.
3. Quote Service requests only `customer:read` from `insure-backend`; the resulting token must carry the Customer Service audience.
4. Customer operations need the Customer Service audience and the `customer:create` and/or `customer:read` scopes.
5. Use Keycloak client scopes or protocol mappers to place the required scope strings in the access-token `scope` claim and to add the corresponding service audience. Do not rely on roles that the application does not map.
6. Configure access-token lifetimes and signing algorithms according to the organization's identity policy. The services accept standard Keycloak-signed JWTs through issuer metadata and do not accept unsigned or opaque tokens.

The permissions enforced by the applications are:

| API operation | Required scope |
|---|---|
| Quote `POST /api/v1/quotes` and `POST /api/v2/quotes` | `quote:create` |
| Quote `GET /api/v1/quotes/{quoteId}` | `quote:read` |
| Customer `POST /api/v1/customers` | `customer:create` |
| Customer `GET /api/v1/customers/{customerId}` | `customer:read` |
| Quote Service → Customer Service lookup | `customer:read` on a client-credentials token with the Customer Service audience |

Internal network location does not grant trust: Customer Service validates the JWT issuer, signature, time claims, audience, and required scope on its API.

## Web UI client

The browser app in `frontend/` signs in with a public client and the authorization code flow with PKCE. There is no client secret, and direct password grants stay disabled.

| Item | Value |
|---|---|
| Client ID | `insure-ui` |
| Redirect URI | `http://localhost:3174/auth/callback` |
| Post-logout redirect | `http://localhost:3174/` |
| Web origin | `http://localhost:3174` |
| Realm roles | `agent`, `customer`; the UI reads them from the `realm_access.roles` claim |
| Client scopes | `quote:create`, `quote:read`, `customer:create`, `customer:read`, assigned as default scopes |
| Audience mappers | one each for `QUOTE_JWT_AUDIENCE` and `CUSTOMER_JWT_AUDIENCE`, so one access token is accepted by both services |

`docker/keycloak/configure-realm.sh` creates anything missing and leaves existing objects alone; run it with `--dry-run` first to see what would change. Besides the `insure-ui` client above, it creates the `insure-backend` client. It reads the admin credentials from the environment or the ignored `.env`, passes them to Keycloak on stdin, and creates a local test user `agent1` with the `agent` role. The generated `agent1` password and the `insure-backend` secret are printed once at creation.

## Backend client

`insure-backend` is the confidential client for server-to-server calls and API testing (Postman). It uses the client-credentials grant; there is no user login.

| Item | Value |
|---|---|
| Client ID | `insure-backend` (override with `BACKEND_CLIENT_ID` when running the script) |
| Client authentication | On, with service accounts enabled; standard flow and direct access grants off |
| Client scopes | `quote:create`, `quote:read`, `customer:create`, `customer:read`, assigned as **optional** scopes |
| Audience mappers | one each for `QUOTE_JWT_AUDIENCE` and `CUSTOMER_JWT_AUDIENCE` |
| Secret | Admin Console > Clients > `insure-backend` > Credentials; printed once by the script when it creates the client |

Because the scopes are optional, a token carries only the scopes its caller asks for with the `scope` parameter. Quote Service asks for `customer:read` and Postman asks for all four, so the same client serves both without giving Quote Service more than it needs. A request with no `scope` gets none of the four and the services answer 403. Both audiences are always present.

Put the secret in the ignored `.env` as `KEYCLOAK_QUOTE_CLIENT_SECRET` and in your Postman environment as `callerClientSecret`. If you change `.env`, restart Quote Service so it picks the value up.

## Postman

Import `postman/insure-ai.postman_collection.json` and one environment file, then select that environment: `postman/insure-ai.local.postman_environment.json` (*Insure AI - Local*, no secret) or your own git-ignored `postman/insure-ai.private.postman_environment.json` (*Insure AI - Local (private)*) that includes the `insure-backend` secret.

| Variable | Value |
|---|---|
| `keycloakBaseUrl`, `realm` | `http://localhost:8280`, `insure-ai` |
| `quoteBaseUrl`, `customerBaseUrl` | `http://localhost:8281`, `http://localhost:8282` |
| `callerClientId` | `insure-backend` |
| `callerClientSecret` | empty in the tracked file. Keep the secret in `postman/insure-ai.private.postman_environment.json` (git-ignored, *Insure AI - Local (private)*) or paste it locally; never commit it |
| `tokenScope` | space-separated scopes requested by *Auth > Get access token* (all four by default) |
| `quoteAudience`, `customerAudience`, `uiClientId`, `agentUsername` | reference values; the collection does not send them |

Run *Auth > Get access token*, which stores `accessToken`, then any other request. To test a missing-permission response, remove a scope from `tokenScope` and fetch a new token.

## Login theme

The login page uses the `insure-ai` theme in `docker/keycloak/themes/insure-ai/login`, mounted read-only into the Keycloak container by `docker-compose.yml`. `docker/keycloak/configure-realm.sh` sets it as the realm's login theme and sets the display name to "Insure AI". After changing the compose file, recreate the container with `docker compose up -d keycloak`; theme files are read without a rebuild because the container runs `start-dev`.

## Compose and independent local startup

From the repository root, create `.env` from `.env.example` if it does not exist. If legacy settings exist in `quote-service/.env`, merge them instead of overwriting either file. Replace placeholders with local-only values, then start only the project-owned infrastructure:

```sh
cp -n .env.example .env
docker compose up -d insure-db keycloak
```

After configuring the realm/clients and environment values, start Quote Service:

```sh
cd quote-service
./mvnw spring-boot:run
```

In another terminal, start Customer Service:

```sh
cd customer-service
./mvnw spring-boot:run
```

The default host ports are Quote API `8281`, Customer API `8282`, PostgreSQL `5433` (one server, a database per service), and this project's Keycloak `8280`. APIs bind locally for development. Services are independently buildable with `./mvnw verify`; the integration tests use ephemeral Testcontainers databases and mock JWT authentication for authorization tests. No Compose command removes or resets a database volume.

For production, supply the environment-specific issuer URI and audiences through the deployment secret/configuration system. Never use development credentials or a development Keycloak realm in production. Missing issuer/audience/client credentials fail startup rather than permitting anonymous API access. The current APIs do not include a gateway; clients must obtain tokens from the organization's Keycloak realm and call the service APIs directly over the deployment's protected network/TLS ingress.
