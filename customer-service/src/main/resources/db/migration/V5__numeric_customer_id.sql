CREATE SEQUENCE customer_id_seq START 1;

-- Existing customers keep a stable, predictable mapping from their customer number:
-- CUS-000008 becomes 10000008. Quote Service applies the same mapping to its stored references.
UPDATE customer_profile
SET customer_id = (10000000 + SUBSTRING(customer_number FROM 5)::BIGINT)::TEXT;

SELECT setval(
    'customer_id_seq',
    GREATEST(10000000, COALESCE((SELECT MAX(customer_id::BIGINT) FROM customer_profile), 10000000))
);

ALTER TABLE customer_profile
    ALTER COLUMN customer_id TYPE VARCHAR(20),
    ALTER COLUMN customer_id SET DEFAULT nextval('customer_id_seq')::TEXT,
    ADD CONSTRAINT customer_profile_id_check CHECK (customer_id ~ '^[0-9]{8,19}$');

ALTER SEQUENCE customer_id_seq OWNED BY customer_profile.customer_id;
