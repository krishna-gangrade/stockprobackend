-- ============================================================
-- V7: Replace fragile users.role ENUM with VARCHAR
-- ============================================================
--
-- Local databases can end up with stale ENUM definitions when
-- they were created before the employee role rename migrations.
-- JPA stores enum names as strings, so a VARCHAR column is both
-- sufficient and much safer for future role additions/renames.

ALTER TABLE users
    MODIFY COLUMN role VARCHAR(32) NOT NULL;

UPDATE users SET role = 'WAREHOUSE_STAFF' WHERE role = 'STAFF';
UPDATE users SET role = 'INVENTORY_MANAGER' WHERE role = 'MANAGER';
UPDATE users SET role = 'PURCHASE_OFFICER' WHERE role = 'OFFICER';

ALTER TABLE users
    MODIFY COLUMN role VARCHAR(32) NOT NULL DEFAULT 'WAREHOUSE_STAFF';
