CREATE TABLE policy_quote (
    quote_id VARCHAR(36) PRIMARY KEY,
    customer_name VARCHAR(80) NOT NULL,
    coverage_type VARCHAR(16) NOT NULL,
    age SMALLINT NOT NULL,
    coverage_amount NUMERIC NOT NULL,
    annual_premium NUMERIC NOT NULL,
    CONSTRAINT policy_quote_customer_name_check
        CHECK (customer_name = BTRIM(customer_name) AND LENGTH(customer_name) BETWEEN 2 AND 80),
    CONSTRAINT policy_quote_coverage_type_check
        CHECK (coverage_type IN ('AUTO', 'HOME', 'LIFE', 'HEALTH')),
    CONSTRAINT policy_quote_age_check
        CHECK (age BETWEEN 18 AND 75),
    CONSTRAINT policy_quote_coverage_amount_check
        CHECK (coverage_amount > 0),
    CONSTRAINT policy_quote_annual_premium_check
        CHECK (annual_premium >= 0)
);
