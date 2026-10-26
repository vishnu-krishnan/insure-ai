# Keycloak authentication and local development

Both Quote Service and Customer Service use Spring Security OAuth2 Resource Server. Spring's Nimbus decoder obtains signing keys from the configured Keycloak issuer metadata and validates signatures, issuer, expiration, and not-before timestamps. Each service additionally requires its configured JWT audience. No application code signs or manually parses tokens.

## Required environment

Configure these values in the ignored repository-root `.env` file, using `.env.example` as the template:

| Variable | Use |
|---|---|
| `KEYCLOAK_PUBLIC_URL` | Public base URL that Keycloak advertises. |
| `KEYCLOAK_ISSUER_URI` | Exact issuer URI for the selected realm, copied from that realm's OpenID configuration. |
| `KEYCLOAK_TOKEN_URI` | Realm's OAuth2 token endpoint, copied from the same OpenID configuration; used by Quote Service's client-credentials flow. |
| `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD` | Local Keycloak bootstrap administrator credentials. |
| `QUOTE_JWT_AUDIENCE` | Audience Quote Service accepts; configure the matching Keycloak audience mapper. |
| `CUSTOMER_JWT_AUDIENCE` | Audience Customer Service accepts; configure the matching Keycloak audience mapper. |
| `KEYCLOAK_QUOTE_CLIENT_ID`, `KEYCLOAK_QUOTE_CLIENT_SECRET` | Confidential client credentials used only for Quote Service's Customer Service client-credentials flow. |

Database credentials in the same file are required by Compose. Do not commit `.env`, client secrets, tokens, signing keys, or production URLs.

## Realm and clients

Create a realm in the local Keycloak Admin Console. The realm name is deliberately not fixed by the application; set `KEYCLOAK_ISSUER_URI` to the exact `issuer` value and `KEYCLOAK_TOKEN_URI` to `token_endpoint` from:

```text
<KEYCLOAK_PUBLIC_URL>/realms/<realm>/.well-known/openid-configuration
```

Configure the realm and its clients as follows:

1. Create a confidential client for Quote Service and enable service accounts/client credentials. Set its client ID and secret in the environment.
2. Create a client for authenticated API callers. Issue access tokens whose `aud` contains the configured Quote Service audience and whose `scope` claim grants `quote:create` and/or `quote:read`.
3. Configure the Quote Service client-credentials service account with `customer:read`; ensure its access token audience contains the Customer Service audience.
4. Create caller access with Customer Service audience and `customer:create` and/or `customer:read` scopes for authorized customer operations.
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
