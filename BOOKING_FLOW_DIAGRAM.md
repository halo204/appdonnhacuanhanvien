# 📊 SƠ ĐỒ BOOKING FLOW - TRƯỚC VÀ SAU KHI SỬA

## ❌ FLOW CŨ (Trước khi sửa)

```
┌─────────────────────────────────────────────────────────────┐
│                        CUSTOMER APP                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Chọn dịch vụ    │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Chọn worker     │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Đặt đơn         │
                    │  Status: PENDING │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  THANH TOÁN NGAY │ ❌ SAI!
                    │  (Chưa có dịch vụ)│
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Worker làm việc │
                    │  (Không rõ status)│
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Hoàn thành      │
                    │  Status: COMPLETED│
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Review          │
                    │  (Bất cứ lúc nào)│ ❌ SAI!
                    └──────────────────┘

VẤN ĐỀ:
❌ Thanh toán trước khi có dịch vụ
❌ Không biết worker có nhận đơn không
❌ Không theo dõi được tiến trình
❌ Review không có validation
❌ Không thể hủy đơn
```

---

## ✅ FLOW MỚI (Sau khi sửa)

```
┌─────────────────────────────────────────────────────────────┐
│                        CUSTOMER APP                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Chọn dịch vụ    │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Chọn worker     │
                    │  (Chỉ available) │ ✅
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Đặt đơn         │
                    │  Status: PENDING │
                    │  + Validation    │ ✅
                    └──────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                         WORKER APP                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Nhận notification│
                    │  "Có đơn mới"    │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Worker ACCEPT   │ ✅ MỚI!
                    │  Status:         │
                    │  WORKER_ASSIGNED │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Worker đang đến │ ✅ MỚI!
                    │  Status:         │
                    │  WORKER_ON_WAY   │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Worker đã đến   │
                    │  Bắt đầu làm     │ ✅ MỚI!
                    │  Status:         │
                    │  IN_PROGRESS     │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Hoàn thành      │
                    │  Status:         │
                    │  COMPLETED       │
                    └──────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        CUSTOMER APP                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  THANH TOÁN      │ ✅ ĐÚNG!
                    │  (Sau khi hoàn   │
                    │   thành dịch vụ) │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Review          │ ✅ ĐÚNG!
                    │  (Trong 7 ngày)  │
                    │  (Chỉ 1 lần)    │
                    └──────────────────┘

LỢI ÍCH:
✅ Thanh toán sau khi có dịch vụ
✅ Worker phải accept đơn
✅ Theo dõi tiến trình chi tiết
✅ Review có validation
✅ Có thể hủy đơn với phí hủy
```

---

## 🔄 CHI TIẾT STATUS TRANSITIONS

```
PENDING
   │
   ├─→ WORKER_ASSIGNED (Worker accept)
   │      │
   │      ├─→ WORKER_ON_WAY (Worker đang đến)
   │      │      │
   │      │      ├─→ IN_PROGRESS (Bắt đầu làm)
   │      │      │      │
   │      │      │      ├─→ COMPLETED (Hoàn thành)
   │      │      │      │
   │      │      │      └─→ CANCELLED_BY_USER (User hủy)
   │      │      │
   │      │      └─→ CANCELLED_BY_USER (User hủy)
   │      │      └─→ CANCELLED_BY_WORKER (Worker hủy)
   │      │
   │      └─→ CANCELLED_BY_USER (User hủy)
   │      └─→ CANCELLED_BY_WORKER (Worker hủy)
   │
   └─→ CANCELLED_BY_USER (User hủy - FREE)
   └─→ CANCELLED_BY_WORKER (Worker hủy)
```

---

## 💰 PHÍ HỦY THEO STATUS

```
┌─────────────────────┬──────────────────┬─────────────────┐
│      STATUS         │   CANCELLED BY   │   PHÍ HỦY      │
├─────────────────────┼──────────────────┼─────────────────┤
│ PENDING             │ USER             │ 0đ (FREE)       │
│ PENDING             │ WORKER           │ Trừ điểm rating │
├─────────────────────┼──────────────────┼─────────────────┤
│ WORKER_ASSIGNED     │ USER             │ 10,000đ         │
│ WORKER_ASSIGNED     │ WORKER           │ Cảnh cáo        │
├─────────────────────┼──────────────────┼─────────────────┤
│ WORKER_ON_WAY       │ USER             │ 20,000đ         │
│ WORKER_ON_WAY       │ WORKER           │ Cảnh cáo nghiêm │
├─────────────────────┼──────────────────┼─────────────────┤
│ IN_PROGRESS         │ USER             │ 50% giá dịch vụ │
│ IN_PROGRESS         │ WORKER           │ Không cho phép  │
├─────────────────────┼──────────────────┼─────────────────┤
│ COMPLETED           │ -                │ Không thể hủy   │
└─────────────────────┴──────────────────┴─────────────────┘
```

---

## 🔐 WORKER AVAILABILITY MANAGEMENT

```
┌─────────────────────────────────────────────────────────────┐
│                    WORKER STATUS FLOW                        │
└─────────────────────────────────────────────────────────────┘

Worker đăng ký
    │
    ▼
┌──────────────────┐
│ Status: PENDING  │
│ is_available: -  │
└──────────────────┘
    │
    ▼ (Admin approve)
┌──────────────────┐
│ Status: APPROVED │
│ is_available: ✅ │ ← Hiển thị trong danh sách
│ current_booking: │
│ NULL             │
└──────────────────┘
    │
    ▼ (Customer đặt đơn)
┌──────────────────┐
│ Status: APPROVED │
│ is_available: ❌ │ ← KHÔNG hiển thị
│ current_booking: │
│ booking_123      │
└──────────────────┘
    │
    ▼ (Hoàn thành đơn)
┌──────────────────┐
│ Status: APPROVED │
│ is_available: ✅ │ ← Hiển thị lại
│ current_booking: │
│ NULL             │
└──────────────────┘
```

---

## 📱 CUSTOMER APP SCREENS

```
┌─────────────────────────────────────────────────────────────┐
│                      HomeScreen                              │
│  - Load services từ MySQL                                   │
│  - Load featured workers (is_available = TRUE)              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  WorkerSelectionScreen                       │
│  - Load workers theo serviceId                              │
│  - Filter: status = APPROVED AND is_available = TRUE        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     BookingScreen                            │
│  - Validate worker available                                │
│  - Validate service exists                                  │
│  - Validate date/time                                       │
│  - Create booking (status = PENDING)                        │
│  - Update worker (is_available = FALSE)                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 RealTimeTrackingScreen                       │
│  - Load booking từ MySQL                                    │
│  - Hiển thị status hiện tại                                 │
│  - Poll status mỗi 5s để update                             │
│  - Hiển thị worker location (GPS - mock)                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (Khi status = COMPLETED)
┌─────────────────────────────────────────────────────────────┐
│                     PaymentScreen                            │
│  - Load booking từ MySQL                                    │
│  - Chỉ hiển thị nếu status = COMPLETED                      │
│  - Chọn phương thức: Cash/MoMo/Bank                         │
│  - Create payment record                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      ReviewScreen                            │
│  - Load booking từ MySQL                                    │
│  - Validate: status = COMPLETED                             │
│  - Validate: chưa review                                    │
│  - Validate: trong vòng 7 ngày                              │
│  - Submit review                                            │
│  - Update worker rating                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 API ENDPOINTS FLOW

```
1. Customer đặt đơn:
   POST /api/bookings
   ├─ Validate worker available
   ├─ Validate service exists
   ├─ Validate date/time
   ├─ Create booking (PENDING)
   ├─ Update worker (is_available = FALSE)
   └─ Return bookingId

2. Worker accept:
   PUT /api/bookings/:id/accept
   ├─ Validate booking exists
   ├─ Validate status = PENDING
   ├─ Update status → WORKER_ASSIGNED
   └─ Log status history

3. Worker update status:
   PUT /api/bookings/:id/status
   ├─ Validate status transition
   ├─ Update booking status
   ├─ If COMPLETED → Release worker
   └─ Log status history

4. Customer thanh toán:
   POST /api/payments
   ├─ Validate booking COMPLETED
   ├─ Validate chưa thanh toán
   ├─ Create payment record
   └─ Update booking payment_status

5. Customer review:
   POST /api/reviews
   ├─ Validate booking COMPLETED
   ├─ Validate chưa review
   ├─ Validate trong 7 ngày
   ├─ Create review
   └─ Update worker rating

6. Hủy đơn:
   PUT /api/bookings/:id/cancel
   ├─ Validate có thể hủy
   ├─ Calculate cancellation fee
   ├─ Update booking status
   ├─ Release worker
   └─ Log status history
```

---

## 🎯 KẾT LUẬN

### Flow cũ:
- Đơn giản nhưng thiếu logic
- Không theo dõi được tiến trình
- Thanh toán sai thời điểm
- Không có validation

### Flow mới:
- ✅ Đầy đủ như app thật (Grab/Uber/Gojek)
- ✅ Theo dõi tiến trình chi tiết
- ✅ Thanh toán đúng thời điểm
- ✅ Validation chặt chẽ
- ✅ Có thể hủy đơn
- ✅ Quản lý worker availability
- ✅ Professional workflow
