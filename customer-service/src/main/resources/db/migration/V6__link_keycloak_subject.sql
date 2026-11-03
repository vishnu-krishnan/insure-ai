-- Links a customer profile to the Keycloak user who owns it (the token "sub").
-- Nullable: profiles registered by an agent stay unlinked until the customer signs up with the same verified email.
ALTER TABLE customer_profile
    ADD COLUMN keycloak_subject VARCHAR(64);

CREATE UNIQUE INDEX customer_profile_keycloak_subject_unique
    ON customer_profile (keycloak_subject)
    WHERE keycloak_subject IS NOT NULL;
