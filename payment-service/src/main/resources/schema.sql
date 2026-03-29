USE payment_db;

CREATE TABLE
    IF NOT EXISTS payment (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        order_id VARCHAR(36) NOT NULL,
        amount DECIMAL(10, 2) NOT NULL,
        status ENUM ('SUCCESS', 'FAILED') NOT NULL,
        payment_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uk_order_id (order_id),
        INDEX idx_order_id (order_id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS wallet (
        user_id BIGINT PRIMARY KEY,
        balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO
    wallet (user_id, balance)
VALUES
    (1001, 5000.00),
    (1002, 500.00),
    (1003, 10000.00) ON DUPLICATE KEY
UPDATE balance =
VALUES
    (balance);