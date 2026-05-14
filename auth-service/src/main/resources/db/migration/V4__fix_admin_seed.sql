-- ============================================================
-- V4: Fix admin seed — remove stale V1 admin so that the
--     BootstrapAdminInitializer can re-create it correctly
--     at next startup with the right email + password.
-- ============================================================
-- After this migration runs:
--   - No ADMIN row exists → existsByRole(ADMIN) = false
--   - BootstrapAdminInitializer will create:
--       email:    admin@stockpro.local  (from BOOTSTRAP_ADMIN_EMAIL env)
--       password: Admin@123             (from BOOTSTRAP_ADMIN_PASSWORD env)
-- ============================================================

DELETE FROM users WHERE email = 'admin@stockpro.com' AND role = 'ADMIN';
