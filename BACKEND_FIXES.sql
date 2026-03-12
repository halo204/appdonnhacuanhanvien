-- ============================================
-- DATABASE SCHEMA UPDATES FOR IMPROVED LOGIC
-- ============================================

-- 1. Add worker availability columns
ALTER TABLE workers 
ADD COLUMN IF NOT EXISTS is_available BOOLEAN DEFAULT TRUE AFTER is_online,
ADD COLUMN IF NOT EXISTS current_booking_id VARCHAR(50) DEFAULT NULL AFTER is_available;

-- 2. Update bookings table for better status flow
ALTER TABLE bookings 
MODIFY COLUMN status ENUM(
    'PENDING',           -- Đơn mới tạo, chờ worker accept
    'WORKER_ASSIGNED',   -- Worker đã accept
    'WORKER_ON_WAY',     -- Worker đang trên đường đến
    'IN_PROGRESS',       -- Đang thực hiện dịch vụ
    'COMPLETED',         -- Hoàn thành
    'CANCELLED_BY_USER', -- User hủy
    'CANCELLED_BY_WORKER'-- Worker hủy
) DEFAULT 'PENDING';

-- 3. Add completed_at if not exists
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS completed_at DATETIME DEFAULT NULL AFTER status;

-- 4. Add cancellation tracking
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS cancelled_at DATETIME DEFAULT NULL AFTER completed_at,
ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(50) DEFAULT NULL AFTER cancelled_at,
ADD COLUMN IF NOT EXISTS cancellation_reason TEXT DEFAULT NULL AFTER cancelled_by,
ADD COLUMN IF NOT EXISTS cancellation_fee DECIMAL(10,2) DEFAULT 0 AFTER cancellation_reason;

-- 5. Add worker acceptance tracking
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS worker_accepted_at DATETIME DEFAULT NULL AFTER created_at,
ADD COLUMN IF NOT EXISTS worker_arrived_at DATETIME DEFAULT NULL AFTER worker_accepted_at,
ADD COLUMN IF NOT EXISTS service_started_at DATETIME DEFAULT NULL AFTER worker_arrived_at;

-- 6. Update payments table for post-payment logic
ALTER TABLE payments
MODIFY COLUMN status ENUM(
    'PENDING',      -- Chờ thanh toán (sau khi COMPLETED)
    'PROCESSING',   -- Đang xử lý
    'COMPLETED',    -- Đã thanh toán
    'FAILED',       -- Thất bại
    'REFUNDED'      -- Đã hoàn tiền
) DEFAULT 'PENDING';

-- 7. Add refund tracking
ALTER TABLE payments
ADD COLUMN IF NOT EXISTS refund_amount DECIMAL(10,2) DEFAULT 0 AFTER amount,
ADD COLUMN IF NOT EXISTS refund_reason TEXT DEFAULT NULL AFTER refund_amount,
ADD COLUMN IF NOT EXISTS refunded_at DATETIME DEFAULT NULL AFTER refund_reason;

-- 8. Add review restrictions
ALTER TABLE reviews
ADD COLUMN IF NOT EXISTS can_edit BOOLEAN DEFAULT TRUE AFTER comment,
ADD COLUMN IF NOT EXISTS edited_at DATETIME DEFAULT NULL AFTER created_at;

-- 9. Create booking status history table
CREATE TABLE IF NOT EXISTS booking_status_history (
    id VARCHAR(50) PRIMARY KEY,
    booking_id VARCHAR(50) NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(50),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- 10. Create cancellation fees table
CREATE TABLE IF NOT EXISTS cancellation_fees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    cancelled_by ENUM('USER', 'WORKER') NOT NULL,
    fee_percentage DECIMAL(5,2) DEFAULT 0,
    fee_fixed DECIMAL(10,2) DEFAULT 0,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Insert default cancellation fee rules
INSERT INTO cancellation_fees (status, cancelled_by, fee_percentage, fee_fixed, description) VALUES
('PENDING', 'USER', 0, 0, 'Hủy trước khi worker accept - Miễn phí'),
('WORKER_ASSIGNED', 'USER', 0, 10000, 'Hủy sau khi worker accept - Phí 10,000đ'),
('WORKER_ON_WAY', 'USER', 0, 20000, 'Hủy khi worker đang đến - Phí 20,000đ'),
('IN_PROGRESS', 'USER', 50, 0, 'Hủy khi đang thực hiện - Phí 50% giá dịch vụ'),
('WORKER_ASSIGNED', 'WORKER', 0, 0, 'Worker hủy - Trừ điểm rating'),
('WORKER_ON_WAY', 'WORKER', 0, 0, 'Worker hủy khi đang đến - Cảnh cáo nghiêm trọng');

-- 11. Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_worker_id ON bookings(worker_id);
CREATE INDEX IF NOT EXISTS idx_workers_available ON workers(is_available, status);
CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_reviews_booking ON reviews(booking_id);

-- 12. Update existing bookings to new status
UPDATE bookings SET status = 'PENDING' WHERE status = 'pending';
UPDATE bookings SET status = 'COMPLETED' WHERE status = 'completed';

-- 13. Set all workers as available initially
UPDATE workers SET is_available = TRUE WHERE current_booking_id IS NULL;
UPDATE workers SET is_available = FALSE WHERE current_booking_id IS NOT NULL;

COMMIT;
