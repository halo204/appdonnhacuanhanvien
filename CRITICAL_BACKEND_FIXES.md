# 🔧 CRITICAL BACKEND FIXES

## Các thay đổi quan trọng cần áp dụng vào `api-server-example.js`

---

## 1. ✅ CẬP NHẬT BOOKING CREATION - VALIDATE ĐẦY ĐỦ

### Thay thế endpoint POST `/api/bookings`:

```javascript
// Create booking with full validation
app.post('/api/bookings', async (req, res) => {
    const connection = await pool.getConnection();
    
    try {
        const { userId, serviceId, workerId, date, time, address, notes } = req.body;
        
        // 1. Validate worker exists and is APPROVED
        const [workers] = await connection.execute(
            'SELECT id, name, status, is_available, current_booking_id FROM workers WHERE id = ?',
            [workerId]
        );
        
        if (!workers[0]) {
            return res.status(400).json({ 
                success: false, 
                message: 'Nhân viên không tồn tại' 
            });
        }
        
        if (workers[0].status !== 'APPROVED') {
            return res.status(400).json({ 
                success: false, 
                message: 'Nhân viên chưa được duyệt' 
            });
        }
        
        if (!workers[0].is_available || workers[0].current_booking_id) {
            return res.status(400).json({ 
                success: false, 
                message: 'Nhân viên đang bận, vui lòng chọn người khác' 
            });
        }
        
        // 2. Validate service exists
        const [services] = await connection.execute(
            'SELECT id, name, price FROM services WHERE id = ?',
            [serviceId]
        );
        
        if (!services[0]) {
            return res.status(400).json({ 
                success: false, 
                message: 'Dịch vụ không tồn tại' 
            });
        }
        
        // 3. Validate worker can do this service
        const [workerServices] = await connection.execute(
            'SELECT * FROM worker_services WHERE worker_id = ? AND service_id = ?',
            [workerId, serviceId]
        );
        
        if (!workerServices[0]) {
            return res.status(400).json({ 
                success: false, 
                message: 'Nhân viên không cung cấp dịch vụ này' 
            });
        }
        
        // 4. Validate date/time
        const bookingDateTime = new Date(`${date} ${time}`);
        const now = new Date();
        
        if (bookingDateTime < now) {
            return res.status(400).json({ 
                success: false, 
                message: 'Không thể đặt lịch trong quá khứ' 
            });
        }
        
        // 5. Start transaction
        await connection.beginTransaction();
        
        const bookingId = generateId('booking');
        
        // 6. Create booking with status = PENDING
        await connection.execute(
            `INSERT INTO bookings (
                id, user_id, service_id, service_name, worker_id, worker_name,
                scheduled_date, scheduled_time, address, notes, price, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')`,
            [
                bookingId, userId, serviceId, services[0].name,
                workerId, workers[0].name, date, time, address,
                notes || '', services[0].price
            ]
        );
        
        // 7. Mark worker as unavailable and assign booking
        await connection.execute(
            `UPDATE workers 
             SET is_available = FALSE, current_booking_id = ?
             WHERE id = ?`,
            [bookingId, workerId]
        );
        
        // 8. Log status change
        await connection.execute(
            `INSERT INTO booking_status_history (id, booking_id, old_status, new_status, changed_by, notes)
             VALUES (?, ?, NULL, 'PENDING', ?, 'Booking created')`,
            [generateId('history'), bookingId, userId]
        );
        
        await connection.commit();
        
        res.json({
            success: true,
            message: 'Đặt dịch vụ thành công',
            bookingId
        });
        
    } catch (error) {
        await connection.rollback();
        console.error('Create booking error:', error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        connection.release();
    }
});
```

---

## 2. ✅ THÊM WORKER ACCEPT BOOKING

### Thêm endpoint mới:

```javascript
// Worker accepts booking
app.put('/api/bookings/:id/accept', async (req, res) => {
    const connection = await pool.getConnection();
    
    try {
        const bookingId = req.params.id;
        const { workerId } = req.body;
        
        // Check booking exists and is PENDING
        const [bookings] = await connection.execute(
            'SELECT * FROM bookings WHERE id = ? AND worker_id = ?',
            [bookingId, workerId]
        );
        
        if (!bookings[0]) {
            return res.status(404).json({ 
                success: false, 
                message: 'Không tìm thấy đơn hàng' 
            });
        }
        
        if (bookings[0].status !== 'PENDING') {
            return res.status(400).json({ 
                success: false, 
                message: 'Đơn hàng không ở trạng thái chờ xác nhận' 
            });
        }
        
        await connection.beginTransaction();
        
        // Update booking status
        await connection.execute(
            `UPDATE bookings 
             SET status = 'WORKER_ASSIGNED', worker_accepted_at = NOW()
             WHERE id = ?`,
            [bookingId]
        );
        
        // Log status change
        await connection.execute(
            `INSERT INTO booking_status_history (id, booking_id, old_status, new_status, changed_by, notes)
             VALUES (?, ?, 'PENDING', 'WORKER_ASSIGNED', ?, 'Worker accepted')`,
            [generateId('history'), bookingId, workerId]
        );
        
        await connection.commit();
        
        res.json({ 
            success: true, 
            message: 'Đã nhận đơn hàng' 
        });
        
    } catch (error) {
        await connection.rollback();
        console.error('Accept booking error:', error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        connection.release();
    }
});
```

---

## 3. ✅ CẬP NHẬT BOOKING STATUS - THEO FLOW ĐÚNG

### Thay thế endpoint PUT `/api/bookings/:id/status`:

```javascript
// Update booking status (with validation)
app.put('/api/bookings/:id/status', async (req, res) => {
    const connection = await pool.getConnection();
    
    try {
        const bookingId = req.params.id;
        const { status, changedBy } = req.body;
        
        // Validate status
        const validStatuses = [
            'PENDING', 'WORKER_ASSIGNED', 'WORKER_ON_WAY', 
            'IN_PROGRESS', 'COMPLETED', 
            'CANCELLED_BY_USER', 'CANCELLED_BY_WORKER'
        ];
        
        if (!validStatuses.includes(status)) {
            return res.status(400).json({ 
                success: false, 
                message: 'Trạng thái không hợp lệ' 
            });
        }
        
        // Get current booking
        const [bookings] = await connection.execute(
            'SELECT * FROM bookings WHERE id = ?',
            [bookingId]
        );
        
        if (!bookings[0]) {
            return res.status(404).json({ 
                success: false, 
                message: 'Không tìm thấy đơn hàng' 
            });
        }
        
        const oldStatus = bookings[0].status;
        
        // Validate status transition
        const validTransitions = {
            'PENDING': ['WORKER_ASSIGNED', 'CANCELLED_BY_USER', 'CANCELLED_BY_WORKER'],
            'WORKER_ASSIGNED': ['WORKER_ON_WAY', 'CANCELLED_BY_USER', 'CANCELLED_BY_WORKER'],
            'WORKER_ON_WAY': ['IN_PROGRESS', 'CANCELLED_BY_USER', 'CANCELLED_BY_WORKER'],
            'IN_PROGRESS': ['COMPLETED', 'CANCELLED_BY_USER'],
            'COMPLETED': [],
            'CANCELLED_BY_USER': [],
            'CANCELLED_BY_WORKER': []
        };
        
        if (!validTransitions[oldStatus].includes(status)) {
            return res.status(400).json({ 
                success: false, 
                message: `Không thể chuyển từ ${oldStatus} sang ${status}` 
            });
        }
        
        await connection.beginTransaction();
        
        // Update booking status
        let updateQuery = 'UPDATE bookings SET status = ?';
        let updateParams = [status];
        
        if (status === 'WORKER_ON_WAY') {
            updateQuery += ', worker_arrived_at = NOW()';
        } else if (status === 'IN_PROGRESS') {
            updateQuery += ', service_started_at = NOW()';
        } else if (status === 'COMPLETED') {
            updateQuery += ', completed_at = NOW()';
        } else if (status.startsWith('CANCELLED')) {
            updateQuery += ', cancelled_at = NOW(), cancelled_by = ?';
            updateParams.push(changedBy);
        }
        
        updateQuery += ' WHERE id = ?';
        updateParams.push(bookingId);
        
        await connection.execute(updateQuery, updateParams);
        
        // If completed, release worker
        if (status === 'COMPLETED' || status.startsWith('CANCELLED')) {
            await connection.execute(
                `UPDATE workers 
                 SET is_available = TRUE, current_booking_id = NULL
                 WHERE id = ?`,
                [bookings[0].worker_id]
            );
        }
        
        // Log status change
        await connection.execute(
            `INSERT INTO booking_status_history (id, booking_id, old_status, new_status, changed_by)
             VALUES (?, ?, ?, ?, ?)`,
            [generateId('history'), bookingId, oldStatus, status, changedBy]
        );
        
        await connection.commit();
        
        res.json({ 
            success: true, 
            message: 'Cập nhật trạng thái thành công' 
        });
        
    } catch (error) {
        await connection.rollback();
        console.error('Update status error:', error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        connection.release();
    }
});
```

---

## 4. ✅ SỬA PAYMENT LOGIC - CHARGE SAU KHI COMPLETED

### Thay thế endpoint POST `/api/payments`:

```javascript
// Create payment AFTER service completed
app.post('/api/payments', async (req, res) => {
    const connection = await pool.getConnection();
    
    try {
        const { bookingId, userId, paymentMethod } = req.body;
        
        // 1. Check booking is COMPLETED
        const [bookings] = await connection.execute(
            'SELECT * FROM bookings WHERE id = ? AND user_id = ?',
            [bookingId, userId]
        );
        
        if (!bookings[0]) {
            return res.status(404).json({ 
                success: false, 
                message: 'Không tìm thấy đơn hàng' 
            });
        }
        
        if (bookings[0].status !== 'COMPLETED') {
            return res.status(400).json({ 
                success: false, 
                message: 'Chỉ có thể thanh toán sau khi dịch vụ hoàn thành' 
            });
        }
        
        // 2. Check if already paid
        const [existingPayments] = await connection.execute(
            'SELECT id FROM payments WHERE booking_id = ? AND status = "COMPLETED"',
            [bookingId]
        );
        
        if (existingPayments.length > 0) {
            return res.status(400).json({ 
                success: false, 
                message: 'Đơn hàng đã được thanh toán' 
            });
        }
        
        await connection.beginTransaction();
        
        const paymentId = generateId('payment');
        const amount = bookings[0].price;
        
        // 3. Create payment record
        let paymentStatus = 'PENDING';
        
        // For CASH, payment is pending until worker confirms
        // For MOMO/BANK, payment is completed immediately (in real app, call payment API)
        if (paymentMethod === 'MOMO' || paymentMethod === 'BANK') {
            paymentStatus = 'COMPLETED';
        }
        
        await connection.execute(
            `INSERT INTO payments (id, booking_id, user_id, amount, payment_method, status)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [paymentId, bookingId, userId, amount, paymentMethod, paymentStatus]
        );
        
        // 4. Update booking payment status
        await connection.execute(
            'UPDATE bookings SET payment_status = ? WHERE id = ?',
            [paymentStatus, bookingId]
        );
        
        // 5. If MOMO/BANK, deduct from wallet (mock)
        if (paymentMethod === 'MOMO' || paymentMethod === 'BANK') {
            // In real app, call MoMo/Bank API here
            console.log(`Processing ${paymentMethod} payment: ${amount}đ`);
        }
        
        await connection.commit();
        
        res.json({
            success: true,
            message: paymentStatus === 'COMPLETED' ? 'Thanh toán thành công' : 'Đơn hàng đã tạo, thanh toán khi hoàn thành',
            paymentId,
            status: paymentStatus
        });
        
    } catch (error) {
        await connection.rollback();
        console.error('Create payment error:', error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        connection.release();
    }
});
```

---

## 5. ✅ SỬA REVIEW VALIDATION - CHỈ SAU KHI COMPLETED

### Thay thế endpoint POST `/api/reviews`:

```javascript
// Create review with validation
app.post('/api/reviews', async (req, res) => {
    const connection = await pool.getConnection();
    
    try {
        const { bookingId, userId, workerId, rating, comment } = req.body;
        
        // 1. Check booking exists and belongs to user
        const [bookings] = await connection.execute(
            'SELECT * FROM bookings WHERE id = ? AND user_id = ?',
            [bookingId, userId]
        );
        
        if (!bookings[0]) {
            return res.status(404).json({ 
                success: false, 
                message: 'Không tìm thấy đơn hàng' 
            });
        }
        
        // 2. Check booking is COMPLETED
        if (bookings[0].status !== 'COMPLETED') {
            return res.status(400).json({ 
                success: false, 
                message: 'Chỉ có thể đánh giá sau khi dịch vụ hoàn thành' 
            });
        }
        
        // 3. Check if already reviewed
        const [existingReviews] = await connection.execute(
            'SELECT id FROM reviews WHERE booking_id = ?',
            [bookingId]
        );
        
        if (existingReviews.length > 0) {
            return res.status(400).json({ 
                success: false, 
                message: 'Bạn đã đánh giá đơn hàng này rồi' 
            });
        }
        
        // 4. Check review within 7 days
        const completedDate = new Date(bookings[0].completed_at);
        const now = new Date();
        const daysDiff = (now - completedDate) / (1000 * 60 * 60 * 24);
        
        if (daysDiff > 7) {
            return res.status(400).json({ 
                success: false, 
                message: 'Chỉ có thể đánh giá trong vòng 7 ngày sau khi hoàn thành' 
            });
        }
        
        // 5. Validate rating
        if (rating < 1 || rating > 5) {
            return res.status(400).json({ 
                success: false, 
                message: 'Đánh giá phải từ 1 đến 5 sao' 
            });
        }
        
        await connection.beginTransaction();
        
        const reviewId = generateId('review');
        
        // 6. Create review
        await connection.execute(
            `INSERT INTO reviews (id, booking_id, user_id, worker_id, rating, comment)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [reviewId, bookingId, userId, workerId, rating, comment || '']
        );
        
        // 7. Update worker rating
        const [stats] = await connection.execute(
            `SELECT COUNT(*) as total, AVG(rating) as avg_rating
             FROM reviews WHERE worker_id = ?`,
            [workerId]
        );
        
        const totalReviews = stats[0].total;
        const avgRating = parseFloat(stats[0].avg_rating).toFixed(2);
        
        await connection.execute(
            `UPDATE workers 
             SET rating = ?, total_reviews = ?
             WHERE id = ?`,
            [avgRating, totalReviews, workerId]
        );
        
        await connection.commit();
        
        res.json({
            success: true,
            message: 'Đánh giá thành công',
            reviewId
        });
        
    } catch (error) {
        await connection.rollback();
        console.error('Create review error:', error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        connection.release();
    }
});
```

---

## 6. ✅ THÊM CANCELLATION LOGIC

### Thêm endpoint mới:

```javascript
// Cancel booking
app.put('/api/bookings/:id/cancel', async (req, res) => {
    const connection = await pool.getConnection();
    
    try {
        const bookingId = req.params.id;
        const { cancelledBy, reason } = req.body; // 'USER' or 'WORKER'
        
        // Get booking
        const [bookings] = await connection.execute(
            'SELECT * FROM bookings WHERE id = ?',
            [bookingId]
        );
        
        if (!bookings[0]) {
            return res.status(404).json({ 
                success: false, 
                message: 'Không tìm thấy đơn hàng' 
            });
        }
        
        const booking = bookings[0];
        
        // Check if can cancel
        if (booking.status === 'COMPLETED' || booking.status.startsWith('CANCELLED')) {
            return res.status(400).json({ 
                success: false, 
                message: 'Không thể hủy đơn hàng này' 
            });
        }
        
        // Calculate cancellation fee
        const [feeRules] = await connection.execute(
            'SELECT * FROM cancellation_fees WHERE status = ? AND cancelled_by = ?',
            [booking.status, cancelledBy]
        );
        
        let cancellationFee = 0;
        if (feeRules[0]) {
            if (feeRules[0].fee_percentage > 0) {
                cancellationFee = booking.price * (feeRules[0].fee_percentage / 100);
            } else {
                cancellationFee = feeRules[0].fee_fixed;
            }
        }
        
        await connection.beginTransaction();
        
        // Update booking
        const newStatus = cancelledBy === 'USER' ? 'CANCELLED_BY_USER' : 'CANCELLED_BY_WORKER';
        
        await connection.execute(
            `UPDATE bookings 
             SET status = ?, cancelled_at = NOW(), cancelled_by = ?, 
                 cancellation_reason = ?, cancellation_fee = ?
             WHERE id = ?`,
            [newStatus, cancelledBy, reason, cancellationFee, bookingId]
        );
        
        // Release worker
        await connection.execute(
            `UPDATE workers 
             SET is_available = TRUE, current_booking_id = NULL
             WHERE id = ?`,
            [booking.worker_id]
        );
        
        // Log status change
        await connection.execute(
            `INSERT INTO booking_status_history (id, booking_id, old_status, new_status, changed_by, notes)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [generateId('history'), bookingId, booking.status, newStatus, cancelledBy, reason]
        );
        
        await connection.commit();
        
        res.json({
            success: true,
            message: 'Đã hủy đơn hàng',
            cancellationFee
        });
        
    } catch (error) {
        await connection.rollback();
        console.error('Cancel booking error:', error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        connection.release();
    }
});
```

---

## 📋 HƯỚNG DẪN ÁP DỤNG

### Bước 1: Cập nhật database
```bash
# Import file SQL vào phpMyAdmin
appdonnhacuanhanvien/BACKEND_FIXES.sql
```

### Bước 2: Backup file hiện tại
```bash
cp api-server-example.js api-server-example.backup.js
```

### Bước 3: Áp dụng các fixes
- Copy từng function ở trên
- Thay thế vào `api-server-example.js`
- Hoặc thêm mới nếu chưa có

### Bước 4: Restart server
```bash
node api-server-example.js
```

---

## ✅ SAU KHI ÁP DỤNG

Hệ thống sẽ có:
- ✅ Booking status flow đầy đủ (6 trạng thái)
- ✅ Worker availability management
- ✅ Payment sau khi COMPLETED
- ✅ Review validation chặt chẽ
- ✅ Cancellation với phí hủy
- ✅ Transaction safety
- ✅ Status history tracking

Logic giống các app dịch vụ thật (Grab, Uber, Gojek)!
