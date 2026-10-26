#!/bin/sh
set -eu

: "${INSURE_DB_USERNAME:?INSURE_DB_USERNAME is required}"
: "${INSURE_DB_PASSWORD:?INSURE_DB_PASSWORD is required}"

if [ "$INSURE_DB_USERNAME" != "insure_app" ]; then
    echo "INSURE_DB_USERNAME must be insure_app" >&2
    exit 1
fi

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
\getenv app_username INSURE_DB_USERNAME
\getenv app_password INSURE_DB_PASSWORD
CREATE ROLE :"app_username" WITH LOGIN PASSWORD :'app_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
GRANT CONNECT ON DATABASE insure_db TO :"app_username";
GRANT USAGE, CREATE ON SCHEMA public TO :"app_username";
SQL
