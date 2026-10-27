CREATE SEQUENCE quote_number_seq START 1;

CREATE FUNCTION next_quote_number() RETURNS VARCHAR AS $$
DECLARE
    next_value BIGINT := nextval('quote_number_seq');
BEGIN
    IF next_value < 1000000 THEN
        RETURN 'QT-' || lpad(next_value::text, 6, '0');
    END IF;
    RETURN 'QT-' || next_value::text;
END
$$ LANGUAGE plpgsql;

ALTER TABLE policy_quote
    ADD COLUMN quote_number VARCHAR(16);

DO $$
DECLARE
    existing RECORD;
BEGIN
    FOR existing IN SELECT quote_id FROM policy_quote ORDER BY quote_id LOOP
        UPDATE policy_quote
        SET quote_number = next_quote_number()
        WHERE quote_id = existing.quote_id;
    END LOOP;
END
$$;

ALTER TABLE policy_quote
    ALTER COLUMN quote_number SET DEFAULT next_quote_number(),
    ALTER COLUMN quote_number SET NOT NULL,
    ADD CONSTRAINT policy_quote_quote_number_unique UNIQUE (quote_number),
    ADD CONSTRAINT policy_quote_quote_number_check CHECK (quote_number ~ '^QT-[0-9]{6,}$');

ALTER SEQUENCE quote_number_seq OWNED BY policy_quote.quote_number;

ALTER TABLE policy_quote
    ADD COLUMN customer_number VARCHAR(16);
