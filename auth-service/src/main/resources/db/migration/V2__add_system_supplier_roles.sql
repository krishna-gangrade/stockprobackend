-- ============================================================
-- V2: Add SYSTEM and SUPPLIER roles to the users table
-- ============================================================
-- MySQL requires ALTER TABLE to expand an ENUM column.
-- The new values are appended; existing rows are unaffected.

ALTER TABLE users
    MODIFY COLUMN role ENUM(
        'STAFF',
        'MANAGER',
        'OFFICER',
        'ADMIN',
        'SYSTEM',
        'SUPPLIER'
    ) NOT NULL DEFAULT 'STAFF';
