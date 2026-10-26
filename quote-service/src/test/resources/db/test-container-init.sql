CREATE ROLE insure_test_app WITH LOGIN PASSWORD 'test-only-insure-app-password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
GRANT CONNECT ON DATABASE insure_test_db TO insure_test_app;
GRANT USAGE, CREATE ON SCHEMA public TO insure_test_app;
