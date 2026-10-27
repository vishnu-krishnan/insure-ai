CREATE SEQUENCE customer_number_seq START 1;

CREATE FUNCTION next_customer_number() RETURNS VARCHAR(16)
    LANGUAGE sql
    AS $$
        SELECT 'CUS-' || CASE WHEN n < 1000000 THEN lpad(n::text, 6, '0') ELSE n::text END
        FROM (SELECT nextval('customer_number_seq') AS n) AS next_value
    $$;

ALTER TABLE customer_profile ADD COLUMN customer_number VARCHAR(16);

UPDATE customer_profile AS target
SET customer_number = numbered.customer_number
FROM (
    SELECT ordered.customer_id, next_customer_number() AS customer_number
    FROM (SELECT customer_id FROM customer_profile ORDER BY customer_id) AS ordered
) AS numbered
WHERE target.customer_id = numbered.customer_id;

ALTER TABLE customer_profile
    ALTER COLUMN customer_number SET DEFAULT next_customer_number(),
    ALTER COLUMN customer_number SET NOT NULL,
    ADD CONSTRAINT customer_profile_number_unique UNIQUE (customer_number),
    ADD CONSTRAINT customer_profile_number_check CHECK (customer_number ~ '^CUS-[0-9]{6,}$');
