-- Government ID the customer supplies on their self-service profile. Both columns are set together or not at all;
-- profiles registered by an agent have none. One ID belongs to one customer.
ALTER TABLE customer_profile
    ADD COLUMN id_type   VARCHAR(20),
    ADD COLUMN id_number VARCHAR(30),
    ADD CONSTRAINT customer_profile_id_document_pair CHECK ((id_type IS NULL) = (id_number IS NULL));

CREATE UNIQUE INDEX customer_profile_id_document_unique
    ON customer_profile (id_type, id_number)
    WHERE id_type IS NOT NULL;
