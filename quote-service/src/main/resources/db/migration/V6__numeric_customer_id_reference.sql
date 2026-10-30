-- Customer Service identifies customers by a numeric ID. Existing customers received 10000000 plus the
-- numeric part of their customer number (CUS-000008 -> 10000008); apply the same mapping to stored references.
-- Quotes saved without a customer number keep their old reference.
UPDATE policy_quote
SET customer_id = (10000000 + SUBSTRING(customer_number FROM 5)::BIGINT)::TEXT
WHERE customer_id IS NOT NULL
  AND customer_id !~ '^[0-9]{8,19}$'
  AND customer_number ~ '^CUS-[0-9]{6,}$';
