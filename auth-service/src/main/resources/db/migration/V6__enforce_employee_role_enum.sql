-- ============================================================
-- V6: Enforce current employee role enum on existing databases
-- ============================================================
--
-- Some local databases were initialized before the role rename
-- migration existed and were then baselined by Flyway, which left
-- the users.role column on the legacy STAFF/MANAGER/OFFICER enum.
-- This migration safely expands the enum, normalizes old values,
-- and reapplies the final role set used by the application.

ALTER TABLE users
    MODIFY COLUMN role ENUM(
        'STAFF',
        'MANAGER',
        'OFFICER',
        'WAREHOUSE_STAFF',
        'INVENTORY_MANAGER',
        'PURCHASE_OFFICER',
        'ADMIN',
        'SYSTEM',
        'SUPPLIER'
    ) NOT NULL DEFAULT 'WAREHOUSE_STAFF';

UPDATE users SET role = 'WAREHOUSE_STAFF' WHERE role = 'STAFF';
UPDATE users SET role = 'INVENTORY_MANAGER' WHERE role = 'MANAGER';
UPDATE users SET role = 'PURCHASE_OFFICER' WHERE role = 'OFFICER';

ALTER TABLE users
    MODIFY COLUMN role ENUM(
        'WAREHOUSE_STAFF',
        'INVENTORY_MANAGER',
        'PURCHASE_OFFICER',
        'ADMIN',
        'SYSTEM',
        'SUPPLIER'
    ) NOT NULL DEFAULT 'WAREHOUSE_STAFF';
