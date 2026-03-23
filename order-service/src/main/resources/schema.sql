-- ============================================================================
-- Order Service Database Initialization Script
-- ============================================================================

-- Create databases (if not already done by Docker environment)
-- CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4;
USE order_db;

-- ============================================================================
-- Orders Table
-- ============================================================================
CREATE TABLE
IF NOT EXISTS orders
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR
(36) UNIQUE NOT NULL COMMENT 'Unique order identifier',
    user_id BIGINT NOT NULL COMMENT 'User who placed the order',
    total_amount DECIMAL
(10,2) NOT NULL COMMENT 'Total order amount',
    status ENUM
('PENDING', 'CONFIRMED', 'CANCELLED', 'FAILED') DEFAULT 'PENDING' COMMENT 'Order status in saga',
    saga_id VARCHAR
(36) COMMENT 'Associated saga ID for tracking',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Order creation timestamp',
    INDEX idx_order_id
(order_id),
    INDEX idx_saga_id
(saga_id),
    INDEX idx_user_id
(user_id),
    INDEX idx_status
(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores orders created in the system';

-- ============================================================================
-- Saga Log Table
-- ============================================================================
CREATE TABLE
IF NOT EXISTS saga_log
(
    saga_id VARCHAR
(36) PRIMARY KEY COMMENT 'Saga identifier',
    current_step VARCHAR
(50) COMMENT 'Current step in saga execution',
    status ENUM
('STARTED','COMPLETED','COMPENSATED','FAILED') DEFAULT 'STARTED' COMMENT 'Saga status',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON
UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tracks saga execution progress and compensation';

-- ============================================================================
-- Insert Sample Data
-- ============================================================================
-- (Leave empty initially - orders and sagas are created at runtime)
