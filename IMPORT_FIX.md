# ✅ ĐÃ SỬA LỖI SQL!

## Vấn đề:
Bảng `bookings` chưa có cột `completed_at` nên SQL báo lỗi khi thêm cột `cancelled_at` AFTER `completed_at`.

## Đã sửa:
Thêm bước tạo cột `completed_at` trước khi thêm các cột cancellation.

## Thử lại:

1. **Mở phpMyAdmin**
   ```
   http://localhost/phpmyadmin
   ```

2. **Chọn database `cleaning_service`**

3. **Click tab "SQL"**

4. **Copy toàn bộ file `BACKEND_FIXES.sql` (đã sửa)**

5. **Paste và click "Go"**

## Nếu vẫn lỗi:

Chạy từng bước một:

### Bước 1: Thêm cột cho workers
```sql
ALTER TABLE workers 
ADD COLUMN IF NOT EXISTS is_available BOOLEAN DEFAULT TRUE AFTER is_online,
ADD COLUMN IF NOT EXISTS current_booking_id VARCHAR(50) DEFAULT NULL AFTER is_available;
```

### Bước 2: Update booking status enum
```sql
ALTER TABLE bookings 
MODIFY COLUMN status ENUM(
    'PENDING',
    'WORKER_ASSIGNED',
    'WORKER_ON_WAY',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED_BY_USER',
    'CANCELLED_BY_WORKER'
) DEFAULT 'PENDING';
```

### Bước 3: Thêm completed_at
```sql
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS completed_at DATETIME DEFAULT NULL AFTER status;
```

### Bước 4: Thêm cancellation tracking
```sql
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS cancelled_at DATETIME DEFAULT NULL AFTER completed_at,
ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(50) DEFAULT NULL AFTER cancelled_at,
ADD COLUMN IF NOT EXISTS cancellation_reason TEXT DEFAULT NULL AFTER cancelled_by,
ADD COLUMN IF NOT EXISTS cancellation_fee DECIMAL(10,2) DEFAULT 0 AFTER cancellation_reason;
```

### Bước 5: Thêm worker acceptance tracking
```sql
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS worker_accepted_at DATETIME DEFAULT NULL AFTER created_at,
ADD COLUMN IF NOT EXISTS worker_arrived_at DATETIME DEFAULT NULL AFTER worker_accepted_at,
ADD COLUMN IF NOT EXISTS service_started_at DATETIME DEFAULT NULL AFTER worker_arrived_at;
```

### Bước 6: Update payments table
```sql
ALTER TABLE payments
MODIFY COLUMN status ENUM(
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'REFUNDED'
) DEFAULT 'PENDING';
```

### Bước 7: Thêm refund tracking
```sql
ALTER TABLE payments
ADD COLUMN IF NOT EXISTS refund_amount DECIMAL(10,2) DEFAULT 0 AFTER amount,
ADD COLUMN IF NOT EXISTS refund_reason TEXT DEFAULT NULL AFTER refund_amount,
ADD COLUMN IF NOT EXISTS refunded_at DATETIME DEFAULT NULL AFTER refund_reason;
```

### Bước 8: Thêm review restrictions
```sql
ALTER TABLE reviews
ADD COLUMN IF NOT EXISTS can_edit BOOLEAN DEFAULT TRUE AFTER comment,
ADD COLUMN IF NOT EXISTS edited_at DATETIME DEFAULT NULL AFTER created_at;
```

### Bước 9: Tạo booking_status_history table
```sql
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
```

### Bước 10: Tạo cancellation_fees table
```sql
CREATE TABLE IF NOT EXISTS cancellation_fees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    cancelled_by ENUM('USER', 'WORKER') NOT NULL,
    fee_percentage DECIMAL(5,2) DEFAULT 0,
    fee_fixed DECIMAL(10,2) DEFAULT 0,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO cancellation_fees (status, cancelled_by, fee_percentage, fee_fixed, description) VALUES
('PENDING', 'USER', 0, 0, 'Hủy trước khi worker accept - Miễn phí'),
('WORKER_ASSIGNED', 'USER', 0, 10000, 'Hủy sau khi worker accept - Phí 10,000đ'),
('WORKER_ON_WAY', 'USER', 0, 20000, 'Hủy khi worker đang đến - Phí 20,000đ'),
('IN_PROGRESS', 'USER', 50, 0, 'Hủy khi đang thực hiện - Phí 50% giá dịch vụ'),
('WORKER_ASSIGNED', 'WORKER', 0, 0, 'Worker hủy - Trừ điểm rating'),
('WORKER_ON_WAY', 'WORKER', 0, 0, 'Worker hủy khi đang đến - Cảnh cáo nghiêm trọng');
```

### Bước 11: Thêm indexes
```sql
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_worker_id ON bookings(worker_id);
CREATE INDEX IF NOT EXISTS idx_workers_available ON workers(is_available, status);
CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_reviews_booking ON reviews(booking_id);
```

### Bước 12: Update existing data
```sql
UPDATE bookings SET status = 'PENDING' WHERE status = 'pending';
UPDATE bookings SET status = 'COMPLETED' WHERE status = 'completed';
UPDATE workers SET is_available = TRUE WHERE current_booking_id IS NULL;
UPDATE workers SET is_available = FALSE WHERE current_booking_id IS NOT NULL;
```

## ✅ Xong!

Sau khi import thành công, restart API server:
```bash
node api-server-example.js
```
