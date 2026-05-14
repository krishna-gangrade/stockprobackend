-- ============================================================
-- V3: Synchronize ENUM role names with Java User.Role enum
-- ============================================================

-- 1. Temporarily expand ENUM to include both OLD and NEW values
ALTER TABLE users
    MODIFY COLUMN role ENUM(
        'STAFF', 'MANAGER', 'OFFICER', 'ADMIN',
        'WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'SYSTEM', 'SUPPLIER'
    ) NOT NULL;

-- 2. Perform the data migration
UPDATE users SET role = 'WAREHOUSE_STAFF' WHERE role = 'STAFF';
UPDATE users SET role = 'INVENTORY_MANAGER' WHERE role = 'MANAGER';
UPDATE users SET role = 'PURCHASE_OFFICER' WHERE role = 'OFFICER';

-- 3. Set the final ENUM values and remove the old ones
ALTER TABLE users
    MODIFY COLUMN role ENUM(
        'WAREHOUSE_STAFF',
        'INVENTORY_MANAGER',
        'PURCHASE_OFFICER',
        'ADMIN',
        'SYSTEM',
        'SUPPLIER'
    ) NOT NULL DEFAULT 'WAREHOUSE_STAFF';
