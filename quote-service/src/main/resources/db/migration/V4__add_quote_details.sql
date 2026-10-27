ALTER TABLE policy_quote
    ADD COLUMN policy_term_years SMALLINT,
    ADD COLUMN payment_frequency VARCHAR(12),
    ADD COLUMN deductible NUMERIC(14, 2),
    ADD COLUMN start_date DATE,
    ADD COLUMN tax_rate NUMERIC(5, 4),
    ADD COLUMN tax_amount NUMERIC(14, 2),
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN created_by VARCHAR(100);

UPDATE policy_quote
SET policy_term_years = 1,
    payment_frequency = 'YEARLY',
    deductible = 0,
    start_date = CURRENT_DATE,
    tax_rate = 0,
    tax_amount = 0,
    created_at = now();

ALTER TABLE policy_quote
    ALTER COLUMN policy_term_years SET NOT NULL,
    ALTER COLUMN payment_frequency SET NOT NULL,
    ALTER COLUMN deductible SET NOT NULL,
    ALTER COLUMN start_date SET NOT NULL,
    ALTER COLUMN tax_rate SET NOT NULL,
    ALTER COLUMN tax_amount SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ADD CONSTRAINT policy_quote_policy_term_years_check CHECK (policy_term_years BETWEEN 1 AND 40),
    ADD CONSTRAINT policy_quote_payment_frequency_check
        CHECK (payment_frequency IN ('MONTHLY', 'QUARTERLY', 'YEARLY')),
    ADD CONSTRAINT policy_quote_deductible_check CHECK (deductible >= 0),
    ADD CONSTRAINT policy_quote_tax_rate_check CHECK (tax_rate BETWEEN 0 AND 1),
    ADD CONSTRAINT policy_quote_tax_amount_check CHECK (tax_amount >= 0);
