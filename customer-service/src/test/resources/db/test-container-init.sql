CREATE ROLE customer_test_app WITH LOGIN PASSWORD 'test-only-customer-app-password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
GRANT CONNECT ON DATABASE customer_test_db TO customer_test_app;
GRANT USAGE, CREATE ON SCHEMA public TO customer_test_app;
