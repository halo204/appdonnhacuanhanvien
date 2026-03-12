-- ============================================
-- SAMPLE BOOKINGS DATA
-- ============================================

USE cleaning_service;

-- Insert sample bookings
INSERT INTO bookings (id, user_id, service_id, service_name, worker_id, worker_name, worker_phone,
                      scheduled_date, scheduled_time, address, status, total_price, distance, icon, is_new) VALUES
('booking_1', 'user_1', 'svc_1', 'Vệ sinh nhà cửa tổng quát', NULL, NULL, NULL,
 UNIX_TIMESTAMP(NOW() + INTERVAL 2 HOUR) * 1000, '14:00', '123 Đường ABC, Quận 1, TP.HCM', 
 'PENDING', 200000, 2.5, '🏠', TRUE),
('booking_2', 'user_2', 'svc_2', 'Vệ sinh bếp', 'worker_1', 'Lê Văn C', '0903456789',
 UNIX_TIMESTAMP(NOW() + INTERVAL 3 HOUR) * 1000, '15:00', '456 Đường XYZ, Quận 2, TP.HCM', 
 'WORKER_ASSIGNED', 150000, 3.2, '🍳', FALSE);

-- Insert sample addresses
INSERT INTO addresses (id, user_id, label, address, is_default) VALUES
('addr_1', 'user_1', 'Nhà riêng', '123 Đường ABC, Quận 1, TP.HCM', TRUE),
('addr_2', 'user_1', 'Văn phòng', '789 Đường DEF, Quận 3, TP.HCM', FALSE),
('addr_3', 'user_2', 'Nhà riêng', '456 Đường XYZ, Quận 2, TP.HCM', TRUE);

-- ============================================
-- VIEWS FOR REPORTING
-- ============================================

-- View: Worker statistics
CREATE OR REPLACE VIEW worker_stats AS
SELECT 
    w.id,
    w.name,
    w.email,
    w.phone,
    w.rating,
    w.total_reviews,
    w.completed_jobs,
    w.total_earnings,
    w.today_earnings,
    w.is_online,
    w.is_available,
    COUNT(DISTINCT b.id) as total_bookings,
    COUNT(DISTINCT CASE WHEN b.status = 'COMPLETED' THEN b.id END) as completed_bookings,
    COUNT(DISTINCT CASE WHEN b.status IN ('PENDING', 'WORKER_ASSIGNED', 'WORKER_ON_WAY') THEN b.id END) as active_bookings
FROM workers w
LEFT JOIN bookings b ON w.id = b.worker_id
GROUP BY w.id;

-- View: Service statistics
CREATE OR REPLACE VIEW service_stats AS
SELECT 
    s.id,
    s.name,
    s.category,
    s.price,
    COUNT(DISTINCT b.id) as total_bookings,
    COUNT(DISTINCT b.user_id) as unique_customers,
    AVG(r.rating) as average_rating,
    COUNT(DISTINCT r.id) as total_reviews
FROM services s
LEFT JOIN bookings b ON s.id = b.service_id
LEFT JOIN reviews r ON s.id = r.service_id
GROUP BY s.id;
