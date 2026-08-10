-- Baseline migration: enable extensions we'll rely on later.
-- pgcrypto gives us gen_random_uuid() for primary keys in later phases.
CREATE EXTENSION IF NOT EXISTS pgcrypto;