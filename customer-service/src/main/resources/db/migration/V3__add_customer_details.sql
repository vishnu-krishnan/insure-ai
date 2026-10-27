ALTER TABLE customer_profile
    ADD COLUMN gender VARCHAR(16),
    ADD COLUMN marital_status VARCHAR(16),
    ADD COLUMN occupation VARCHAR(80),
    ADD COLUMN annual_income NUMERIC(14, 2),
    ADD COLUMN address_line1 VARCHAR(120),
    ADD COLUMN address_line2 VARCHAR(120),
    ADD COLUMN address_city VARCHAR(80),
    ADD COLUMN address_state VARCHAR(80),
    ADD COLUMN address_postal_code VARCHAR(12),
    ADD COLUMN address_country CHAR(2),
    ADD COLUMN nominee_name VARCHAR(80),
    ADD COLUMN nominee_relationship VARCHAR(40),
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE customer_profile
    ADD CONSTRAINT customer_profile_gender_check
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNSPECIFIED')),
    ADD CONSTRAINT customer_profile_marital_status_check
        CHECK (marital_status IN ('SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED')),
    ADD CONSTRAINT customer_profile_annual_income_check CHECK (annual_income >= 0),
    ADD CONSTRAINT customer_profile_address_country_check CHECK (address_country ~ '^[A-Z]{2}$'),
    ADD CONSTRAINT customer_profile_address_complete_check CHECK (
        (address_line1 IS NULL AND address_line2 IS NULL AND address_city IS NULL AND address_state IS NULL
            AND address_postal_code IS NULL AND address_country IS NULL)
        OR (address_line1 IS NOT NULL AND address_city IS NOT NULL AND address_state IS NOT NULL
            AND address_postal_code IS NOT NULL AND address_country IS NOT NULL)
    ),
    ADD CONSTRAINT customer_profile_nominee_complete_check CHECK (
        (nominee_name IS NULL AND nominee_relationship IS NULL)
        OR (nominee_name IS NOT NULL AND nominee_relationship IS NOT NULL)
    );
