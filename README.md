# Insurance AI Assistant

## Prerequisites

- Java 17
- Docker Engine and Docker Compose v2
- The Maven Wrapper included in this project

## Local database setup

Create the root-level environment file if one does not already exist. If you already have local settings in `quote-service/.env`, merge them into the root file rather than overwriting either file:

```sh
cp -n .env.example .env
```

Keep `.env` private and replace all password placeholders with distinct strong local passwords. Hexadecimal values generated with `openssl rand -hex 32` work well with Compose interpolation. Configure the Keycloak realm values as described in `docs/keycloak.md`. `INSURE_DB_USERNAME` must remain `insure_app`; the local host URL is `jdbc:postgresql://localhost:5433/insure_db`.

Start only this project's PostgreSQL service:

```sh
docker compose up -d insure-db
docker compose ps
```

The `insure-db` service runs `postgres:16` in the `insure-postgres` container. It publishes PostgreSQL only on `127.0.0.1:5433` and persists its data in the named `insure-postgres-data` volume at `/var/lib/postgresql/data`. The health check reports whether PostgreSQL is ready.

The server hosts one database per service: `insure_db` for Quote Service and `customer_db` for Customer Service. Each service connects with its own non-superuser role (`insure_app` and `customer_app`), which has connection privileges and `USAGE`/`CREATE` on the `public` schema of its own database so Flyway can maintain the schema history and migrations. Connection to the other service's database is denied, so a service cannot read another service's tables. Migrated application tables are owned by the service's role. The separate `insure_admin` initialization account is a database administrator; do not use it for application connections.

The initialization scripts in `docker/postgres` run only when the data volume is created. To add the Customer Service database to a volume that already exists, run the idempotent script once (the password is taken from your shell environment, not the command line):

```sh
export CUSTOMER_DB_USERNAME=customer_app
export CUSTOMER_DB_PASSWORD='<the value from .env>'
docker compose exec -T -e CUSTOMER_DB_USERNAME -e CUSTOMER_DB_PASSWORD insure-db sh -s < docker/postgres/02-create-customer-database.sh
```

## Running the application

The application reads the root `.env` and supports the legacy service-local `.env` files; service-local values take precedence. Start Quote Service from its module directory:

```sh
cd quote-service
./mvnw spring-boot:run
```

Spring Boot uses `INSURE_DB_URL`, `INSURE_DB_USERNAME`, and `INSURE_DB_PASSWORD`. Flyway applies versioned migrations at application startup; migrations are not created through `schema.sql`. If the application is later run inside Docker Compose, set `INSURE_DB_URL` to `jdbc:postgresql://insure-db:5432/insure_db` because `localhost` inside the application container refers to that container.

## Quote API and data ownership

The `quote-service` Maven module is independently buildable and runnable (`spring.application.name=quote-service`). It owns quote creation and retrieval, the `policy_quote` table, and the Flyway migrations under `quote-service/src/main/resources/db/migration`. The service accesses that table only through its JDBC persistence adapter; other services must use the HTTP contract rather than query the table directly.

The versioned API currently supports:

- `POST /api/v1/quotes` — create a quote (existing behavior).
- `GET /api/v1/quotes/{quoteId}` — retrieve a quote by ID; returns `404` Problem Details when it does not exist.
- `GET /api/v1/quotes?page=0&size=20` — list quotes ordered by `quoteId`. `page` is zero-based; `size` must be between 1 and 100. The response carries `content`, `page`, `size`, `totalElements` and `totalPages`.
- `POST /api/v2/quotes` — create a quote using an existing `customerId` from Customer Service.

The v1 endpoints use the `QuoteRequestDto`/`QuoteResponseDto` HTTP contract and the v2 endpoint uses `CreateCustomerQuoteRequestDto`/`CustomerQuoteResponseDto`. Domain types remain inside the service boundary.

Customer Service is an independent Maven application in `customer-service`. It exclusively owns customer profiles and its Flyway migrations. It exposes `POST /api/v1/customers` and `GET /api/v1/customers/{customerId}`. Quote Service v2 obtains customer details through that HTTP API using an OAuth2 client-credentials token; it does not access Customer Service tables. Existing Quote v1 requests and rows remain supported; the nullable `customer_id` introduced by migration V2 leaves existing records unchanged.

## Local services and Keycloak

From the repository root, start local infrastructure after configuring the ignored `.env` from `.env.example`:

```sh
docker compose up -d insure-db keycloak
```

All host ports are loopback-only and were chosen to avoid clashing with other projects on the same machine: PostgreSQL `5433` (one server hosting both service databases), Keycloak `8280`, Quote Service `8281` and Customer Service `8282`. Each can be changed through `.env` (`INSURE_DB_HOST_PORT`, `KEYCLOAK_PORT`, `QUOTE_SERVICE_PORT`, `CUSTOMER_SERVICE_PORT`); keep the matching URLs in `.env` in sync. Run the applications independently from their respective directories using `./mvnw spring-boot:run`. Direct local API requests require valid Keycloak JWTs.

The Compose project name is pinned to `insure-ai`, and every container, volume and network is prefixed with `insure-`, so this stack does not collide with other Compose projects on the machine.

Keycloak realm, issuer, audiences, clients, scopes, client credentials, and token setup are documented in `docs/keycloak.md`. No realm or client is created automatically. Both services fail startup when required JWT configuration is missing; JWT validation is never disabled.

## API documentation

Both services publish OpenAPI documentation through springdoc: Swagger UI at `/swagger-ui.html` and the specification at `/v3/api-docs` (for example `http://localhost:8281/swagger-ui.html` for Quote Service). The documentation endpoints are disabled unless `API_DOCS_ENABLED=true`; `.env.example` enables them for local development. Keep them disabled in shared or production environments. Use *Authorize* in Swagger UI with a bearer token from Keycloak to call the secured operations.

## Postman collection

`postman/insure-ai.postman_collection.json` covers every endpoint of both services, including error cases, saved example responses and test scripts. Import it together with `postman/insure-ai.local.postman_environment.json`, select the *Insure AI - Local* environment, fill `callerClientId` and `callerClientSecret` (never commit them), and run *Auth > Get access token* before the other requests.

## Errors and logging

All error responses are RFC 9457 Problem Details (`application/problem+json`) with a stable `code` (for example `QUOTE_NOT_FOUND`, `VALIDATION_FAILED`, `UNAUTHENTICATED`, `INTERNAL_ERROR`), the request's `correlationId`, and, for validation failures, an `errors` list with one entry per invalid field. Unexpected failures return a generic message and are logged once, with the stack trace, on the server.

Every request carries an `X-Correlation-Id`: a caller-supplied value of 1-64 characters from `A-Za-z0-9._-` is reused, otherwise one is generated. It is returned in the response header, included in error bodies and written into every log line. Each request is logged once as `METHOD /path -> status (duration)`; query strings, headers and bodies are never logged.

## Tests

Run unit and PostgreSQL integration tests, followed by packaging, in each independently buildable module:

```sh
(cd quote-service && ./mvnw clean verify)
(cd customer-service && ./mvnw clean verify)
```

Unit tests do not require PostgreSQL. Integration tests use Testcontainers and a separate, ephemeral PostgreSQL 16 instance; they do not connect to the local Compose database. Docker must be available, and these required tests fail rather than silently skip when a container cannot start.

## Safe database lifecycle and backups

Stop the database without deleting data:

```sh
docker compose stop insure-db
```

Starting the service again, restarting its container, or removing and recreating the container preserves data because the database directory is on `insure-postgres-data`. Do not remove that named volume for routine cleanup: **deleting `insure-postgres-data` permanently deletes the local database contents**.

Create a custom-format backup in the project directory:

```sh
docker compose exec -T insure-db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > insure_db.dump
docker compose exec -T insure-db sh -c 'pg_dump -U "$POSTGRES_USER" -d customer_db -Fc' > customer_db.dump
```

Restore that backup only when intentionally replacing the contents of this database, and retain a separate backup before doing so:

```sh
docker compose exec -T insure-db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists' < insure_db.dump
docker compose exec -T insure-db sh -c 'pg_restore -U "$POSTGRES_USER" -d customer_db --clean --if-exists' < customer_db.dump
```

The dump contains application data and should be stored securely. Keep `.env` and backup files out of version control.
