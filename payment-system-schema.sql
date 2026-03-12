-- ============================================
-- PAYMENT SYSTEM DATABASE SCHEMA
-- Thêm hệ thống thanh toán và ví điện tử
-- ============================================

USE cleaning_service;

-- 1. Bảng user_wallets (Ví điện tử)
CREATE TABLE IF NOT EXISTS user_wallets (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    momo_balance DECIMAL(10,2) DEFAULT 5000000.00,
    bank_balance DECIMAL(10,2) DEFAULT 10000000.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng payments (Thanh toán)
CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(50) PRIMARY KEY,
    booking_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method ENUM('CASH', 'MOMO', 'BANK') NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    transaction_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_booking (booking_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng wallet_transactions (Lịch sử giao dịch ví)
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    wallet_type ENUM('MOMO', 'BANK') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    type ENUM('DEBIT', 'CREDIT') NOT NULL COMMENT 'DEBIT=Trừ tiền, CREDIT=Cộng tiền',
    description TEXT,
    booking_id VARCHAR(50),
    balance_before DECIMAL(10,2) NOT NULL,
    balance_after DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_wallet_type (wallet_type),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Cập nhật bảng bookings
ALTER TABLE bookings 
ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) COMMENT 'CASH, MOMO, BANK',
ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, COMPLETED, FAILED',
ADD COLUMN IF NOT EXISTS payment_id VARCHAR(50);

-- 5. Tạo/Cập nhật bảng reviews
CREATE TABLE IF NOT EXISTS reviews (
    id VARCHAR(50) PRIMARY KEY,
    booking_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    worker_id VARCHAR(50) NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE CASCADE,
    UNIQUE KEY unique_booking_review (booking_id),
    INDEX idx_worker (worker_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- SAMPLE DATA
-- ============================================

-- Tạo ví cho users hiện có
INSERT INTO user_wallets (id, user_id, momo_balance, bank_balance) 
SELECT 
    CONCAT('wallet_', id), 
    id, 
    5000000.00, 
    10000000.00
FROM users
ON DUPLICATE KEY UPDATE 
    momo_balance = VALUES(momo_balance),
    bank_balance = VALUES(bank_balance);

-- ============================================
-- VIEWS
-- ============================================

-- View: Thống kê thanh toán
CREATE OR REPLACE VIEW payment_stats AS
SELECT 
    DATE(created_at) as payment_date,
    method,
    COUNT(*) as total_payments,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount
FROM payments
WHERE status = 'COMPLETED'
GROUP BY DATE(created_at), method;

-- View: Lịch sử giao dịch user
CREATE OR REPLACE VIEW user_transaction_history AS
SELECT 
    wt.id,
    wt.user_id,
    u.name as user_name,
    wt.wallet_type,
    wt.amount,
    wt.type,
    wt.description,
    wt.balance_before,
    wt.balance_after,
    wt.created_at,
    b.service_name
FROM wallet_transactions wt
JOIN users u ON wt.user_id = u.id
LEFT JOIN bookings b ON wt.booking_id = b.id
ORDER BY wt.created_at DESC;

-- View: Worker ratings
CREATE OR REPLACE VIEW worker_ratings AS
SELECT 
    w.id as worker_id,
    w.name as worker_name,
    COUNT(r.id) as total_reviews,
    AVG(r.rating) as average_rating,
    SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END) as five_star,
    SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END) as four_star,
    SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END) as three_star,
    SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END) as two_star,
    SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END) as one_star
FROM workers w
LEFT JOIN reviews r ON w.id = r.worker_id
GROUP BY w.id, w.name;

SELECT 'Payment system schema created successfully!' as message;
SELECT COUNT(*) as total_wallets FROM user_wallets;
