CREATE TABLE customer_profile (
    customer_id VARCHAR(36) PRIMARY KEY,
    full_name VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(16) NOT NULL,
    date_of_birth DATE NOT NULL,
    CONSTRAINT customer_profile_name_check
        CHECK (full_name = BTRIM(full_name) AND LENGTH(full_name) BETWEEN 2 AND 80),
    CONSTRAINT customer_profile_email_check
        CHECK (email = BTRIM(email) AND LENGTH(email) BETWEEN 3 AND 254),
    CONSTRAINT customer_profile_phone_check
        CHECK (phone ~ '^\+[1-9][0-9]{7,14}$'),
    CONSTRAINT customer_profile_dob_check
        CHECK (date_of_birth < CURRENT_DATE)
);
