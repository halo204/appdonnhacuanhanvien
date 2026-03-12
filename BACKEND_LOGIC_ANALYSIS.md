# 🔍 PHÂN TÍCH LOGIC BACKEND - SO SÁNH VỚI CÁC APP DỊCH VỤ THẬT

## 📱 Tham khảo: Grab, Uber, Gojek, GoViet

---

## ❌ VẤN ĐỀ PHÁT HIỆN

### 1. **BOOKING STATUS FLOW - THIẾU CÁC TRẠNG THÁI QUAN TRỌNG**

#### ❌ Hiện tại:
```
PENDING → COMPLETED
```

#### ✅ Nên có (như Grab/Uber):
```
PENDING → WORKER_ASSIGNED → WORKER_ON_WAY → IN_PROGRESS → COMPLETED
                                                         → CANCELLED
```

**Giải thích**:
- `PENDING`: Đơn mới tạo, chưa có worker nhận
- `WORKER_ASSIGNED`: Worker đã nhận đơn
- `WORKER_ON_WAY`: Worker đang trên đường đến
- `IN_PROGRESS`: Worker đang thực hiện dịch vụ
- `COMPLETED`: Hoàn thành
- `CANCELLED`: Đã hủy (bởi customer hoặc worker)

---

### 2. **PAYMENT TIMING - SAI LOGIC**

#### ❌ Hiện tại:
- Customer thanh toán NGAY SAU KHI ĐẶT ĐƠN
- Chưa có dịch vụ mà đã trả tiền

#### ✅ Nên có (như Grab/Uber):

**Option 1: Pre-authorization (Grab, Uber)**
```
1. Đặt đơn → Hold tiền (pre-auth)
2. Dịch vụ hoàn thành → Charge tiền thật
3. Nếu hủy → Release hold
```

**Option 2: Post-payment (GoViet, Gojek)**
```
1. Đặt đơn → Không charge
2. Dịch vụ hoàn thành → Mới charge tiền
3. Tiền mặt → Trả sau khi hoàn thành
```

**Đề xuất cho app này**: Dùng Option 2 (đơn giản hơn)
- MoMo/Bank: Charge sau khi status = COMPLETED
- Cash: Trả trực tiếp cho worker sau khi COMPLETED

---

### 3. **WORKER ASSIGNMENT - THIẾU LOGIC TỰ ĐỘNG**

#### ❌ Hiện tại:
- Customer chọn worker thủ công
- Không có hệ thống matching tự động

#### ✅ Nên có (như Grab/Uber):

**Option 1: Auto-assign (Grab, Uber)**
```
1. Customer đặt đơn (không chọn worker)
2. Hệ thống tìm worker gần nhất, available
3. Gửi notification cho worker
4. Worker accept/reject trong 30s
5. Nếu reject → Tìm worker khác
```

**Option 2: Manual selection (hiện tại)**
```
1. Customer chọn worker từ danh sách
2. Worker nhận notification
3. Worker accept/reject
```

**Đề xuất**: Giữ manual selection (đơn giản hơn) nhưng thêm:
- Worker phải ACCEPT đơn trước khi bắt đầu
- Timeout nếu worker không accept trong 5 phút
- Customer có thể hủy nếu worker không accept

---

### 4. **CANCELLATION POLICY - CHƯA CÓ**

#### ❌ Hiện tại:
- Không có chức năng hủy đơn
- Không có phí hủy

#### ✅ Nên có (như Grab/Uber):

```
Customer hủy:
- Trước khi worker accept: FREE
- Sau khi worker accept nhưng chưa đến: Phí 10,000đ
- Sau khi worker đang đến: Phí 20,000đ
- Sau khi worker đã đến: Phí 50% giá dịch vụ

Worker hủy:
- Trước khi đến: Cảnh cáo
- Sau khi đã accept: Trừ điểm rating
- Hủy nhiều lần: Suspend account
```

---

### 5. **RATING RESTRICTIONS - CHƯA VALIDATE**

#### ❌ Hiện tại:
- Có thể review bất cứ lúc nào
- Có thể review nhiều lần

#### ✅ Nên có (như Grab/Uber):

```javascript
// Check if booking is completed
if (booking.status !== 'COMPLETED') {
    return res.status(400).json({ 
        message: 'Chỉ có thể đánh giá sau khi dịch vụ hoàn thành' 
    });
}

// Check if already reviewed
const existingReview = await getReview(bookingId);
if (existingReview) {
    return res.status(400).json({ 
        message: 'Bạn đã đánh giá đơn hàng này rồi' 
    });
}

// Check if review within 7 days
const completedDate = new Date(booking.completed_at);
const now = new Date();
const daysDiff = (now - completedDate) / (1000 * 60 * 60 * 24);
if (daysDiff > 7) {
    return res.status(400).json({ 
        message: 'Chỉ có thể đánh giá trong vòng 7 ngày sau khi hoàn thành' 
    });
}
```

---

### 6. **REFUND LOGIC - CHƯA CÓ**

#### ❌ Hiện tại:
- Không có logic hoàn tiền

#### ✅ Nên có (như Grab/Uber):

```
Trường hợp hoàn tiền:
1. Worker hủy sau khi accept → Hoàn 100%
2. Dịch vụ không đạt yêu cầu → Hoàn 50-100%
3. Worker không đến → Hoàn 100%
4. Khiếu nại được chấp nhận → Hoàn theo quyết định admin
```

---

### 7. **WORKER AVAILABILITY - CHƯA QUẢN LÝ**

#### ❌ Hiện tại:
- Tất cả workers luôn hiển thị
- Không biết worker có available không

#### ✅ Nên có (như Grab/Uber):

```sql
ALTER TABLE workers ADD COLUMN is_available BOOLEAN DEFAULT TRUE;
ALTER TABLE workers ADD COLUMN current_booking_id VARCHAR(50);
```

```javascript
// Worker chỉ hiển thị nếu:
// 1. Status = APPROVED
// 2. is_available = TRUE
// 3. current_booking_id = NULL (không có đơn đang làm)

// Khi worker accept đơn:
UPDATE workers SET 
    is_available = FALSE,
    current_booking_id = ?
WHERE id = ?;

// Khi hoàn thành đơn:
UPDATE workers SET 
    is_available = TRUE,
    current_booking_id = NULL
WHERE id = ?;
```

---

### 8. **BOOKING VALIDATION - THIẾU NHIỀU**

#### ❌ Hiện tại:
- Chỉ validate basic fields
- Không validate business rules

#### ✅ Nên có (như Grab/Uber):

```javascript
// Validate worker availability
const [worker] = await pool.execute(
    'SELECT is_available, status FROM workers WHERE id = ?',
    [workerId]
);

if (!worker[0] || worker[0].status !== 'APPROVED') {
    return res.status(400).json({ 
        message: 'Nhân viên không khả dụng' 
    });
}

if (!worker[0].is_available) {
    return res.status(400).json({ 
        message: 'Nhân viên đang bận, vui lòng chọn người khác' 
    });
}

// Validate service exists
const [service] = await pool.execute(
    'SELECT price FROM services WHERE id = ?',
    [serviceId]
);

if (!service[0]) {
    return res.status(400).json({ 
        message: 'Dịch vụ không tồn tại' 
    });
}

// Validate date/time
const bookingDate = new Date(`${date} ${time}`);
const now = new Date();

if (bookingDate < now) {
    return res.status(400).json({ 
        message: 'Không thể đặt lịch trong quá khứ' 
    });
}

// Validate worker can do this service
const [workerService] = await pool.execute(
    'SELECT * FROM worker_services WHERE worker_id = ? AND service_id = ?',
    [workerId, serviceId]
);

if (!workerService[0]) {
    return res.status(400).json({ 
        message: 'Nhân viên không cung cấp dịch vụ này' 
    });
}
```

---

### 9. **TRANSACTION SAFETY - THIẾU**

#### ❌ Hiện tại:
- Nhiều operations không dùng transaction
- Có thể bị inconsistent data

#### ✅ Nên có (như Grab/Uber):

```javascript
// Ví dụ: Create booking phải atomic
const connection = await pool.getConnection();
await connection.beginTransaction();

try {
    // 1. Create booking
    await connection.execute('INSERT INTO bookings ...');
    
    // 2. Update worker availability
    await connection.execute('UPDATE workers SET is_available = FALSE ...');
    
    // 3. Create payment record
    await connection.execute('INSERT INTO payments ...');
    
    await connection.commit();
} catch (error) {
    await connection.rollback();
    throw error;
} finally {
    connection.release();
}
```

---

### 10. **NOTIFICATION SYSTEM - CHƯA CÓ**

#### ❌ Hiện tại:
- Không có notification

#### ✅ Nên có (như Grab/Uber):

```
Customer notifications:
- Worker đã nhận đơn
- Worker đang trên đường đến
- Worker đã đến
- Dịch vụ hoàn thành
- Nhắc nhở đánh giá

Worker notifications:
- Có đơn mới
- Customer hủy đơn
- Nhận được đánh giá
```

---

## 🎯 ƯU TIÊN SỬA CHỮA

### 🔴 CRITICAL (Phải sửa ngay):
1. ✅ Booking status flow (thêm các trạng thái)
2. ✅ Payment timing (charge sau khi COMPLETED)
3. ✅ Worker availability management
4. ✅ Booking validation (business rules)
5. ✅ Rating restrictions (chỉ sau COMPLETED)

### 🟡 IMPORTANT (Nên sửa):
6. ⚠️ Worker acceptance flow
7. ⚠️ Cancellation policy
8. ⚠️ Transaction safety
9. ⚠️ Refund logic

### 🟢 NICE TO HAVE (Có thể làm sau):
10. ⚠️ Notification system
11. ⚠️ Auto-assign workers
12. ⚠️ GPS tracking
13. ⚠️ Chat system

---

## 📝 KẾT LUẬN

Backend hiện tại đã có **cấu trúc tốt** nhưng thiếu nhiều **business logic quan trọng** so với các app dịch vụ thật.

Cần bổ sung:
- ✅ Status flow đầy đủ
- ✅ Payment logic đúng
- ✅ Validation chặt chẽ
- ✅ Worker availability
- ⚠️ Cancellation & refund
- ⚠️ Notifications

Sau khi sửa các vấn đề CRITICAL, app sẽ hoạt động giống các app dịch vụ thật!
