#!/bin/sh
# Creates the Customer Service database and role on the shared server and keeps each
# service's database private to its own role. Safe to run more than once.
set -eu

: "${CUSTOMER_DB_USERNAME:?CUSTOMER_DB_USERNAME is required}"
: "${CUSTOMER_DB_PASSWORD:?CUSTOMER_DB_PASSWORD is required}"

if [ "$CUSTOMER_DB_USERNAME" != "customer_app" ]; then
    echo "CUSTOMER_DB_USERNAME must be customer_app" >&2
    exit 1
fi

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
\getenv app_username CUSTOMER_DB_USERNAME
\getenv app_password CUSTOMER_DB_PASSWORD

SELECT format(
    'CREATE ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT',
    :'app_username', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_username')
\gexec

SELECT format('CREATE DATABASE customer_db OWNER %I', :'app_username')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'customer_db')
\gexec

REVOKE CONNECT ON DATABASE insure_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE customer_db FROM PUBLIC;
GRANT CONNECT ON DATABASE customer_db TO :"app_username";
SQL

psql --username "$POSTGRES_USER" --dbname customer_db <<'SQL'
\getenv app_username CUSTOMER_DB_USERNAME
GRANT USAGE, CREATE ON SCHEMA public TO :"app_username";
SQL
