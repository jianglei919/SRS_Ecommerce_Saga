-- ============================================================================
-- Inventory Service Database Initialization Script
-- ============================================================================

-- Create database (if not already done by Docker environment)
-- CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4;
USE inventory_db;

-- ============================================================================
-- Inventory Table
-- ============================================================================
CREATE TABLE
IF NOT EXISTS inventory
(
    product_id BIGINT PRIMARY KEY COMMENT 'Product identifier',
    product_name VARCHAR
(100) COMMENT 'Product name',
    stock INT DEFAULT 1000 COMMENT 'Total available stock',
    reserved INT DEFAULT 0 COMMENT 'Currently reserved for pending orders',
    INDEX idx_product_name
(product_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product inventory with stock and reserved quantities';

-- ============================================================================
-- Inventory Log Table
-- ============================================================================
CREATE TABLE
IF NOT EXISTS inventory_log
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR
(36) NOT NULL COMMENT 'Associated order ID',
    product_id BIGINT NOT NULL COMMENT 'Product being reserved/released',
    quantity INT NOT NULL COMMENT 'Quantity of product',
    action ENUM
('RESERVE','RELEASE') NOT NULL COMMENT 'Type of action (reserve or release)',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Action timestamp',
    FOREIGN KEY
(product_id) REFERENCES inventory
(product_id),
    INDEX idx_order_id
(order_id),
    INDEX idx_product_id
(product_id),
    INDEX idx_action
(action),
    INDEX idx_timestamp
(timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit log for all inventory transactions';

-- ============================================================================
-- Insert Sample Products
-- ============================================================================
INSERT INTO inventory
    (product_id, product_name, stock, reserved)
VALUES
    (1, 'iPhone 16', 1000, 0),
    (2, 'MacBook Pro', 500, 0),
    (3, 'iPad Air', 750, 0),
    (4, 'Apple Watch', 300, 0)
ON DUPLICATE KEY
UPDATE
    product_name = VALUES
(product_name),
    stock = VALUES
(stock);

-- Verify data
SELECT *
FROM inventory;
