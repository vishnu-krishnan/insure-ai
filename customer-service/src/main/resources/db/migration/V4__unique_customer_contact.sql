-- Plain (non-IF NOT EXISTS) unique indexes: the migration aborts if duplicate contacts already exist.
CREATE UNIQUE INDEX customer_profile_email_unique ON customer_profile (lower(email));
CREATE UNIQUE INDEX customer_profile_phone_unique ON customer_profile (phone);
