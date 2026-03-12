-- ============================================
-- IMPORT COMPLETE DATABASE WITH DATA
-- ============================================

-- Drop database if exists and recreate
DROP DATABASE IF EXISTS cleaning_service;
CREATE DATABASE cleaning_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cleaning_service;

-- ============================================
-- CREATE TABLES
-- ============================================

-- Service Categories
CREATE TABLE service_categories (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(10),
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Services
CREATE TABLE services (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    duration INT NOT NULL COMMENT 'Duration in minutes',
    image_url VARCHAR(255),
    icon VARCHAR(10),
    category_id VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES service_categories(id)
);

-- Users (Customers)
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Workers
CREATE TABLE workers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    avatar_url VARCHAR(255),
    rating DECIMAL(3,2) DEFAULT 0.00,
    total_reviews INT DEFAULT 0,
    completed_jobs INT DEFAULT 0,
    total_earnings DECIMAL(10,2) DEFAULT 0.00,
    today_earnings DECIMAL(10,2) DEFAULT 0.00,
    is_online BOOLEAN DEFAULT FALSE,
    is_available BOOLEAN DEFAULT TRUE,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Worker Services (Many-to-Many)
CREATE TABLE worker_services (
    worker_id VARCHAR(50),
    service_id VARCHAR(50),
    PRIMARY KEY (worker_id, service_id),
    FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
);

-- Bookings
CREATE TABLE bookings (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    service_id VARCHAR(50) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    worker_id VARCHAR(50),
    worker_name VARCHAR(100),
    worker_phone VARCHAR(20),
    scheduled_date BIGINT NOT NULL COMMENT 'Timestamp in milliseconds',
    scheduled_time VARCHAR(10),
    address TEXT NOT NULL,
    status ENUM('PENDING', 'WORKER_ASSIGNED', 'WORKER_ON_WAY', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    total_price DECIMAL(10,2) NOT NULL,
    distance DECIMAL(5,2),
    icon VARCHAR(10),
    is_new BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (service_id) REFERENCES services(id),
    FOREIGN KEY (worker_id) REFERENCES workers(id)
);

-- Reviews
CREATE TABLE reviews (
    id VARCHAR(50) PRIMARY KEY,
    booking_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    worker_id VARCHAR(50) NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (worker_id) REFERENCES workers(id)
);

-- Addresses
CREATE TABLE addresses (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    label VARCHAR(50),
    address TEXT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ============================================
-- INSERT SAMPLE DATA
-- ============================================

-- Service Categories
INSERT INTO service_categories (id, name, description, icon, display_order) VALUES
('cat_1', 'Vệ sinh nhà cửa', 'Dịch vụ vệ sinh tổng quát', '🏠', 1),
('cat_2', 'Vệ sinh chuyên sâu', 'Vệ sinh chi tiết từng khu vực', '✨', 2),
('cat_3', 'Giặt ủi', 'Dịch vụ giặt ủi quần áo', '👔', 3);

-- Services
INSERT INTO services (id, name, description, price, duration, icon, category_id) VALUES
('svc_1', 'Vệ sinh nhà cửa tổng quát', 'Quét dọn, lau chùi toàn bộ nhà', 200000.00, 120, '🏠', 'cat_1'),
('svc_2', 'Vệ sinh bếp', 'Vệ sinh bếp, rửa chén, lau dọn', 150000.00, 90, '🍳', 'cat_1'),
('svc_3', 'Vệ sinh phòng tắm', 'Vệ sinh toilet, nhà tắm sạch sẽ', 120000.00, 60, '🚿', 'cat_1'),
('svc_4', 'Lau kính cửa sổ', 'Lau sạch kính cửa sổ, ban công', 100000.00, 60, '🪟', 'cat_2'),
('svc_5', 'Giặt thảm, sofa', 'Giặt thảm và sofa chuyên nghiệp', 300000.00, 180, '🛋️', 'cat_2'),
('svc_6', 'Giặt ủi quần áo', 'Giặt và ủi quần áo theo kg', 50000.00, 120, '👔', 'cat_3');

-- Users with real bcrypt hash for "password123"
INSERT INTO users (id, email, password, name, phone, address) VALUES
('user_1', 'customer1@example.com', '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G', 'Nguyễn Văn A', '0901234567', '123 Đường ABC, Quận 1, TP.HCM'),
('user_2', 'customer2@example.com', '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G', 'Trần Thị B', '0902345678', '456 Đường XYZ, Quận 2, TP.HCM');

-- Workers with real bcrypt hash for "password123"
INSERT INTO workers (id, name, email, password, phone, rating, total_reviews, completed_jobs, is_available, status) VALUES
('worker_1', 'Lê Văn C', 'worker1@example.com', '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G', '0903456789', 4.8, 120, 115, TRUE, 'APPROVED'),
('worker_2', 'Phạm Thị D', 'worker2@example.com', '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G', '0904567890', 4.9, 95, 92, TRUE, 'APPROVED'),
('worker_3', 'Hoàng Văn E', 'worker3@example.com', '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G', '0905678901', 4.7, 80, 78, TRUE, 'APPROVED'),
('worker_4', 'Võ Thị F', 'worker4@example.com', '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G', '0906789012', 4.6, 65, 63, FALSE, 'APPROVED');

-- Worker Services (assign workers to services)
INSERT INTO worker_services (worker_id, service_id) VALUES
('worker_1', 'svc_1'),
('worker_1', 'svc_2'),
('worker_2', 'svc_1'),
('worker_2', 'svc_3'),
('worker_3', 'svc_4'),
('worker_3', 'svc_5'),
('worker_4', 'svc_6');

-- Sample Bookings
INSERT INTO bookings (id, user_id, service_id, service_name, worker_id, worker_name, worker_phone,
                      scheduled_date, scheduled_time, address, status, total_price, distance, icon, is_new) VALUES
('booking_1', 'user_1', 'svc_1', 'Vệ sinh nhà cửa tổng quát', 'worker_1', 'Lê Văn C', '0903456789',
 UNIX_TIMESTAMP(NOW() + INTERVAL 2 HOUR) * 1000, '14:00', '123 Đường ABC, Quận 1, TP.HCM', 
 'WORKER_ASSIGNED', 200000, 2.5, '🏠', TRUE);

-- Sample Addresses
INSERT INTO addresses (id, user_id, label, address, is_default) VALUES
('addr_1', 'user_1', 'Nhà riêng', '123 Đường ABC, Quận 1, TP.HCM', TRUE),
('addr_2', 'user_1', 'Văn phòng', '789 Đường DEF, Quận 3, TP.HCM', FALSE),
('addr_3', 'user_2', 'Nhà riêng', '456 Đường XYZ, Quận 2, TP.HCM', TRUE);

SELECT 'Database imported successfully!' as message;
SELECT COUNT(*) as total_services FROM services;
SELECT COUNT(*) as total_users FROM users;
SELECT COUNT(*) as total_workers FROM workers;
