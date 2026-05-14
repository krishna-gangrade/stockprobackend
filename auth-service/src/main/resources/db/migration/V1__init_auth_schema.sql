-- ============================================================
-- StockPro Auth Service — Initial Schema
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100)        NOT NULL,
    email         VARCHAR(150)        NOT NULL UNIQUE,
    password_hash VARCHAR(255)        NOT NULL,
    phone         VARCHAR(20),
    role          ENUM('STAFF','MANAGER','OFFICER','ADMIN') NOT NULL DEFAULT 'STAFF',
    department    VARCHAR(100),
    is_active     BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at    DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME,
    version       BIGINT              NOT NULL DEFAULT 0,

    INDEX idx_users_email      (email),
    INDEX idx_users_role       (role),
    INDEX idx_users_is_active  (is_active),
    INDEX idx_users_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Audit log table for all user-management actions
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id    BIGINT,
    action      VARCHAR(60)  NOT NULL,
    target_id   BIGINT,
    details     TEXT,
    ip_address  VARCHAR(45),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_actor     (actor_id),
    INDEX idx_audit_action    (action),
    INDEX idx_audit_created   (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Seed default ADMIN account  (password: Admin@1234)
-- bcrypt hash rounds=12
-- ============================================================
INSERT IGNORE INTO users (full_name, email, password_hash, role, is_active)
VALUES ('System Admin',
        'admin@stockpro.com',
        '$2a$12$K8Fk5Pn1Vv4AQsWmGqj0.uFRWlTZaXpEbGoP/3CQ2wVuWvHGvTey',
        'ADMIN',
        TRUE);
