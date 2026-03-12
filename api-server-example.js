// ============================================
// SIMPLE API SERVER FOR CLEANING SERVICE
// Node.js + Express + MySQL
// ============================================

const express = require('express');
const mysql = require('mysql2/promise');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Serve static files (HTML, CSS, JS)
app.use(express.static(__dirname));

// Database connection pool
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'cleaning_service',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// Helper function to generate ID
const generateId = (prefix) => {
    return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

// ============================================
// WORKER ENDPOINTS
// ============================================

// Register worker
app.post('/api/workers/register', async (req, res) => {
    try {
        const { name, email, password, phone } = req.body;
        
        // Check if email already exists
        const [existingWorkers] = await pool.execute(
            'SELECT id FROM workers WHERE email = ?',
            [email]
        );
        
        if (existingWorkers.length > 0) {
            return res.status(400).json({ 
                success: false, 
                message: 'Email đã được sử dụng. Vui lòng dùng email khác.' 
            });
        }
        
        // Hash password
        const hashedPassword = await bcrypt.hash(password, 10);
        const workerId = generateId('worker');
        
        const [result] = await pool.execute(
            `INSERT INTO workers (id, name, email, password, phone, status) 
             VALUES (?, ?, ?, ?, ?, 'PENDING')`,
            [workerId, name, email, hashedPassword, phone]
        );
        
        // Generate token
        const token = jwt.sign({ workerId, email }, process.env.JWT_SECRET || 'secret');
        
        // Get worker data
        const [workers] = await pool.execute(
            'SELECT id, name, email, phone, completed_jobs, rating, total_earnings, today_earnings, is_online, status FROM workers WHERE id = ?',
            [workerId]
        );
        
        res.json({
            success: true,
            message: 'Đăng ký thành công! Tài khoản đang chờ admin duyệt.',
            worker: workers[0],
            token
        });
    } catch (error) {
        console.error('Register error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Login worker
app.post('/api/workers/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        
        const [workers] = await pool.execute(
            'SELECT * FROM workers WHERE email = ?',
            [email]
        );
        
        if (workers.length === 0) {
            return res.status(401).json({ success: false, message: 'Email không tồn tại' });
        }
        
        const worker = workers[0];
        const validPassword = await bcrypt.compare(password, worker.password);
        
        if (!validPassword) {
            return res.status(401).json({ success: false, message: 'Mật khẩu không đúng' });
        }
        
        // Generate token
        const token = jwt.sign({ workerId: worker.id, email }, process.env.JWT_SECRET || 'secret');
        
        // Remove password from response
        delete worker.password;
        
        res.json({
            success: true,
            message: 'Đăng nhập thành công',
            worker,
            token
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Get ALL workers (for admin) - including PENDING, APPROVED, REJECTED
app.get('/api/workers/all', async (req, res) => {
    try {
        const [workers] = await pool.execute(
            `SELECT id, name, email, phone, avatar_url, rating, total_reviews, 
                    completed_jobs, is_online, is_available, status, created_at, updated_at
             FROM workers 
             ORDER BY created_at DESC`
        );
        
        res.json(workers);
    } catch (error) {
        console.error('Get all workers error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Update worker status (for admin approval)
app.put('/api/workers/:id/status', async (req, res) => {
    try {
        const { status } = req.body;
        const workerId = req.params.id;
        
        // Validate status
        const validStatuses = ['PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'];
        if (!validStatuses.includes(status)) {
            return res.status(400).json({ 
                success: false, 
                message: 'Invalid status. Must be: PENDING, APPROVED, REJECTED, or SUSPENDED' 
            });
        }
        
        await pool.execute(
            'UPDATE workers SET status = ?, updated_at = NOW() WHERE id = ?',
            [status, workerId]
        );
        
        res.json({ 
            success: true, 
            message: `Worker status updated to ${status}` 
        });
    } catch (error) {
        console.error('Update worker status error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Get worker by ID
app.get('/api/workers/:id', async (req, res) => {
    try {
        const [workers] = await pool.execute(
            'SELECT id, name, email, phone, completed_jobs, average_rating, total_earnings, today_earnings, is_online FROM workers WHERE id = ?',
            [req.params.id]
        );
        
        if (workers.length === 0) {
            return res.status(404).json({ success: false, message: 'Worker not found' });
        }
        
        res.json(workers[0]);
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// Update worker online status
app.put('/api/workers/:id/online', async (req, res) => {
    try {
        const { isOnline } = req.body;
        
        await pool.execute(
            'UPDATE workers SET is_online = ? WHERE id = ?',
            [isOnline, req.params.id]
        );
        
        res.json({ success: true, message: 'Cập nhật trạng thái thành công' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// ============================================
// JOB ENDPOINTS
// ============================================

// Get pending jobs
app.get('/api/jobs/pending', async (req, res) => {
    try {
        const [jobs] = await pool.execute(
            `SELECT id, service_id as serviceId, service_name as serviceName, 
                    user_id as customerId, address, scheduled_date as scheduledDate, 
                    total_price as price, status, distance, icon, is_new as isNew,
                    'Khách hàng' as customerName, '0901234567' as customerPhone
             FROM bookings 
             WHERE status = 'PENDING' AND worker_id IS NULL
             ORDER BY created_at DESC`
        );
        
        // Convert scheduled_date from milliseconds to Date
        const formattedJobs = jobs.map(job => ({
            ...job,
            scheduledDate: new Date(parseInt(job.scheduledDate))
        }));
        
        res.json({ success: true, jobs: formattedJobs });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// Get worker jobs
app.get('/api/jobs/worker/:workerId', async (req, res) => {
    try {
        const [jobs] = await pool.execute(
            `SELECT b.id, b.service_id as serviceId, b.service_name as serviceName,
                    b.user_id as customerId, u.name as customerName, u.phone as customerPhone,
                    b.address, b.scheduled_date as scheduledDate, b.total_price as price,
                    b.status, b.distance, b.icon, b.is_new as isNew
             FROM bookings b
             LEFT JOIN users u ON b.user_id = u.id
             WHERE b.worker_id = ? AND b.status != 'COMPLETED' AND b.status != 'CANCELLED'
             ORDER BY b.scheduled_date ASC`,
            [req.params.workerId]
        );
        
        const formattedJobs = jobs.map(job => ({
            ...job,
            scheduledDate: new Date(parseInt(job.scheduledDate))
        }));
        
        res.json({ success: true, jobs: formattedJobs });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// Accept job
app.put('/api/jobs/:jobId/accept', async (req, res) => {
    try {
        const { workerId } = req.body;
        
        // Get worker info
        const [workers] = await pool.execute(
            'SELECT name, phone FROM workers WHERE id = ?',
            [workerId]
        );
        
        if (workers.length === 0) {
            return res.status(404).json({ success: false, message: 'Worker not found' });
        }
        
        const worker = workers[0];
        
        // Update booking
        await pool.execute(
            `UPDATE bookings 
             SET worker_id = ?, worker_name = ?, worker_phone = ?, 
                 status = 'WORKER_ASSIGNED', is_new = FALSE
             WHERE id = ?`,
            [workerId, worker.name, worker.phone, req.params.jobId]
        );
        
        res.json({ success: true, message: 'Đã nhận đơn thành công' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// Update job status
// Update job status with validation
app.put('/api/jobs/:jobId/status', async (req, res) => {
    const connection = await pool.getConnection();
    
    try {
        const bookingId = req.params.jobId;
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
            updateParams.push(changedBy || 'SYSTEM');
        }
        
        updateQuery += ' WHERE id = ?';
        updateParams.push(bookingId);
        
        await connection.execute(updateQuery, updateParams);
        
        // If completed or cancelled, release worker
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
            [generateId('history'), bookingId, oldStatus, status, changedBy || 'SYSTEM']
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

// Complete job
app.put('/api/jobs/:jobId/complete', async (req, res) => {
    try {
        // Get booking info
        const [bookings] = await pool.execute(
            'SELECT worker_id, total_price, service_name FROM bookings WHERE id = ?',
            [req.params.jobId]
        );
        
        if (bookings.length === 0) {
            return res.status(404).json({ success: false, message: 'Booking not found' });
        }
        
        const booking = bookings[0];
        
        // Update booking status
        await pool.execute(
            'UPDATE bookings SET status = ?, actual_end_time = ? WHERE id = ?',
            ['COMPLETED', Date.now(), req.params.jobId]
        );
        
        // Update worker earnings and completed jobs
        await pool.execute(
            `UPDATE workers 
             SET completed_jobs = completed_jobs + 1,
                 total_earnings = total_earnings + ?,
                 today_earnings = today_earnings + ?
             WHERE id = ?`,
            [booking.total_price, booking.total_price, booking.worker_id]
        );
        
        // Add to earnings history
        const earningId = generateId('earning');
        await pool.execute(
            `INSERT INTO earnings_history (id, worker_id, booking_id, service_name, amount, completed_at)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [earningId, booking.worker_id, req.params.jobId, booking.service_name, booking.total_price, Date.now()]
        );
        
        res.json({ success: true, message: 'Hoàn thành đơn hàng' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// ============================================
// USER/CUSTOMER ENDPOINTS
// ============================================

// Register user
app.post('/api/users/register', async (req, res) => {
    try {
        const { name, email, password, phone } = req.body;
        
        const hashedPassword = await bcrypt.hash(password, 10);
        const userId = generateId('user');
        
        await pool.execute(
            'INSERT INTO users (id, name, email, password, phone) VALUES (?, ?, ?, ?, ?)',
            [userId, name, email, hashedPassword, phone]
        );
        
        const token = jwt.sign({ userId, email }, process.env.JWT_SECRET || 'secret');
        
        res.json({
            success: true,
            message: 'Đăng ký thành công',
            user: { id: userId, name, email, phone },
            token
        });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// Login user
app.post('/api/users/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        
        const [users] = await pool.execute(
            'SELECT * FROM users WHERE email = ?',
            [email]
        );
        
        if (users.length === 0) {
            return res.status(401).json({ success: false, message: 'Email không tồn tại' });
        }
        
        const user = users[0];
        const validPassword = await bcrypt.compare(password, user.password);
        
        if (!validPassword) {
            return res.status(401).json({ success: false, message: 'Mật khẩu không đúng' });
        }
        
        const token = jwt.sign({ userId: user.id, email }, process.env.JWT_SECRET || 'secret');
        
        delete user.password;
        
        res.json({
            success: true,
            message: 'Đăng nhập thành công',
            user,
            token
        });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

// Get all services
app.get('/api/services', async (req, res) => {
    try {
        const [services] = await pool.execute(
            'SELECT * FROM services WHERE is_active = TRUE ORDER BY category_id, name'
        );
        
        res.json(services);
    } catch (error) {
        console.error('Get services error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Get available workers
app.get('/api/workers', async (req, res) => {
    try {
        const { serviceId } = req.query;
        
        let query = `SELECT w.id, w.name, w.phone, w.email, w.avatar_url, w.rating, 
                            w.total_reviews, w.is_available, w.completed_jobs, w.status
                     FROM workers w`;
        let params = [];
        
        // If serviceId is provided, filter by service
        if (serviceId) {
            query += ` INNER JOIN worker_services ws ON w.id = ws.worker_id
                       WHERE ws.service_id = ? AND w.status = 'APPROVED' AND w.is_available = TRUE`;
            params.push(serviceId);
        } else {
            query += ` WHERE w.status = 'APPROVED' AND w.is_available = TRUE`;
        }
        
        query += ` ORDER BY w.rating DESC, w.completed_jobs DESC`;
        
        const [workers] = await pool.execute(query, params);
        
        res.json(workers);
    } catch (error) {
        console.error('Get workers error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Create booking with full validation
app.post('/api/bookings', async (req, res) => {
    const connection = await pool.getConnection();
    
    try {
        console.log('📥 Received booking request:', JSON.stringify(req.body, null, 2));
        
        const { 
            userId, 
            serviceId, 
            serviceName, 
            workerId,
            workerName,
            workerPhone,
            date, 
            time,
            address, 
            totalPrice, 
            notes,
            status 
        } = req.body;
        
        // Validate required fields
        if (!userId || !serviceId || !serviceName || !address || !totalPrice) {
            return res.status(400).json({ 
                success: false, 
                message: 'Missing required fields' 
            });
        }
        
        // 1. Validate worker exists and is APPROVED
        if (workerId) {
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
        
        // 3. Validate worker can do this service (if worker selected)
        if (workerId) {
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
        }
        
        // 4. Validate date/time
        let scheduledDate;
        if (typeof date === 'string') {
            scheduledDate = new Date(date).getTime();
        } else if (typeof date === 'number') {
            scheduledDate = date;
        } else {
            scheduledDate = Date.now();
        }
        
        const bookingDateTime = new Date(scheduledDate);
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
                id, user_id, service_id, service_name, worker_id, worker_name, worker_phone,
                scheduled_date, scheduled_time, address, total_price, notes, status, is_new
            )
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', TRUE)`,
            [
                bookingId, 
                userId, 
                serviceId, 
                serviceName, 
                workerId || null,
                workerName || null,
                workerPhone || null,
                scheduledDate,
                time || '',
                address, 
                totalPrice, 
                notes || ''
            ]
        );
        
        // 7. Mark worker as unavailable and assign booking (if worker selected)
        if (workerId) {
            await connection.execute(
                `UPDATE workers 
                 SET is_available = FALSE, current_booking_id = ?
                 WHERE id = ?`,
                [bookingId, workerId]
            );
        }
        
        // 8. Log status change
        await connection.execute(
            `INSERT INTO booking_status_history (id, booking_id, old_status, new_status, changed_by, notes)
             VALUES (?, ?, NULL, 'PENDING', ?, 'Booking created')`,
            [generateId('history'), bookingId, userId]
        );
        
        await connection.commit();
        
        console.log('✅ Booking created successfully:', bookingId);
        
        res.json({
            success: true,
            message: 'Đặt dịch vụ thành công',
            id: bookingId,
            userId,
            serviceId,
            serviceName,
            workerId: workerId || '',
            workerName: workerName || '',
            workerPhone: workerPhone || '',
            date: new Date(scheduledDate),
            time: time || '',
            address,
            totalPrice,
            notes: notes || '',
            status: 'PENDING',
            createdAt: new Date()
        });
        
    } catch (error) {
        await connection.rollback();
        console.error('❌ Create booking error:', error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        connection.release();
    }
});

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

// Get user bookings
app.get('/api/bookings/user/:userId', async (req, res) => {
    try {
        console.log('📋 Getting bookings for user:', req.params.userId);
        
        const [bookings] = await pool.execute(
            `SELECT b.*, 
                    w.name as workerName, 
                    w.phone as workerPhone, 
                    w.rating as workerRating
             FROM bookings b
             LEFT JOIN workers w ON b.worker_id = w.id
             WHERE b.user_id = ?
             ORDER BY b.scheduled_date DESC`,
            [req.params.userId]
        );
        
        // Format response to match Android model
        const formattedBookings = bookings.map(booking => ({
            id: booking.id,
            userId: booking.user_id,
            serviceId: booking.service_id,
            serviceName: booking.service_name,
            workerId: booking.worker_id || '',
            workerName: booking.workerName || '',
            workerPhone: booking.workerPhone || '',
            workerImageUrl: '',
            date: new Date(parseInt(booking.scheduled_date)),
            time: booking.scheduled_time || '',
            address: booking.address,
            notes: booking.notes || '',
            status: booking.status,
            totalPrice: parseFloat(booking.total_price),
            createdAt: booking.created_at
        }));
        
        console.log(`✅ Found ${formattedBookings.length} bookings for user ${req.params.userId}`);
        res.json(formattedBookings);
    } catch (error) {
        console.error('❌ Get user bookings error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Get booking by ID
app.get('/api/bookings/:id', async (req, res) => {
    try {
        console.log('📖 Getting booking:', req.params.id);
        
        const [bookings] = await pool.execute(
            `SELECT b.*, 
                    w.name as workerName, 
                    w.phone as workerPhone, 
                    w.rating as workerRating,
                    s.name as serviceName,
                    s.price as servicePrice
             FROM bookings b
             LEFT JOIN workers w ON b.worker_id = w.id
             LEFT JOIN services s ON b.service_id = s.id
             WHERE b.id = ?`,
            [req.params.id]
        );
        
        if (bookings.length === 0) {
            console.log('❌ Booking not found:', req.params.id);
            return res.status(404).json({ 
                success: false, 
                message: 'Booking not found' 
            });
        }
        
        const booking = bookings[0];
        
        // Format response to match Android model
        const response = {
            id: booking.id,
            userId: booking.user_id,
            serviceId: booking.service_id,
            serviceName: booking.service_name || booking.serviceName,
            workerId: booking.worker_id || '',
            workerName: booking.worker_name || booking.workerName || '',
            workerPhone: booking.worker_phone || booking.workerPhone || '',
            workerImageUrl: '',
            date: new Date(parseInt(booking.scheduled_date)),
            time: booking.scheduled_time || '',
            address: booking.address,
            notes: booking.notes || '',
            status: booking.status,
            totalPrice: parseFloat(booking.total_price),
            createdAt: booking.created_at
        };
        
        console.log('✅ Booking found:', response.serviceName);
        res.json(response);
    } catch (error) {
        console.error('❌ Get booking error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

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
                cancellationFee = booking.total_price * (feeRules[0].fee_percentage / 100);
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
        if (booking.worker_id) {
            await connection.execute(
                `UPDATE workers 
                 SET is_available = TRUE, current_booking_id = NULL
                 WHERE id = ?`,
                [booking.worker_id]
            );
        }
        
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

// ============================================
// WALLET ENDPOINTS
// ============================================

// Get user wallet
app.get('/api/wallets/:userId', async (req, res) => {
    try {
        const [wallets] = await pool.execute(
            'SELECT * FROM user_wallets WHERE user_id = ?',
            [req.params.userId]
        );
        
        if (wallets.length === 0) {
            // Create wallet if not exists
            const walletId = generateId('wallet');
            await pool.execute(
                'INSERT INTO user_wallets (id, user_id) VALUES (?, ?)',
                [walletId, req.params.userId]
            );
            
            const [newWallet] = await pool.execute(
                'SELECT * FROM user_wallets WHERE id = ?',
                [walletId]
            );
            return res.json(newWallet[0]);
        }
        
        res.json(wallets[0]);
    } catch (error) {
        console.error('Get wallet error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Get wallet transactions
app.get('/api/wallets/:userId/transactions', async (req, res) => {
    try {
        const [transactions] = await pool.execute(
            `SELECT * FROM wallet_transactions 
             WHERE user_id = ? 
             ORDER BY created_at DESC 
             LIMIT 50`,
            [req.params.userId]
        );
        
        res.json(transactions);
    } catch (error) {
        console.error('Get transactions error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Top up wallet (admin only - for testing)
app.post('/api/wallets/:userId/topup', async (req, res) => {
    try {
        const { walletType, amount } = req.body;
        const userId = req.params.userId;
        
        // Get current balance
        const [wallets] = await pool.execute(
            'SELECT * FROM user_wallets WHERE user_id = ?',
            [userId]
        );
        
        if (wallets.length === 0) {
            return res.status(404).json({ success: false, message: 'Wallet not found' });
        }
        
        const wallet = wallets[0];
        const column = walletType === 'MOMO' ? 'momo_balance' : 'bank_balance';
        const currentBalance = parseFloat(wallet[column]);
        const newBalance = currentBalance + parseFloat(amount);
        
        // Update balance
        await pool.execute(
            `UPDATE user_wallets SET ${column} = ? WHERE user_id = ?`,
            [newBalance, userId]
        );
        
        // Create transaction record
        const transactionId = generateId('txn');
        await pool.execute(
            `INSERT INTO wallet_transactions 
             (id, user_id, wallet_type, amount, type, description, balance_before, balance_after)
             VALUES (?, ?, ?, ?, 'CREDIT', 'Nạp tiền vào ví', ?, ?)`,
            [transactionId, userId, walletType, amount, currentBalance, newBalance]
        );
        
        res.json({ 
            success: true, 
            message: 'Top up successful',
            newBalance 
        });
    } catch (error) {
        console.error('Top up error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// ============================================
// PAYMENT ENDPOINTS
// ============================================

// Create payment
// Create payment AFTER service completed
app.post('/api/payments', async (req, res) => {
    const connection = await pool.getConnection();
    
    try {
        const { bookingId, userId, paymentMethod } = req.body;
        const method = paymentMethod || req.body.method; // Support both field names
        
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
        const transactionId = generateId('txn');
        const amount = bookings[0].total_price;
        
        // 3. Create payment record
        let paymentStatus = 'PENDING';
        
        // For CASH, payment is pending until worker confirms
        // For MOMO/BANK, payment is completed immediately (in real app, call payment API)
        if (method === 'MOMO' || method === 'BANK') {
            paymentStatus = 'COMPLETED';
            
            // Mock: Deduct from wallet (in real app, call MoMo/Bank API)
            const [wallets] = await connection.execute(
                'SELECT * FROM user_wallets WHERE user_id = ?',
                [userId]
            );
            
            if (wallets.length > 0) {
                const wallet = wallets[0];
                const column = method === 'MOMO' ? 'momo_balance' : 'bank_balance';
                const currentBalance = parseFloat(wallet[column]);
                
                if (currentBalance >= parseFloat(amount)) {
                    const newBalance = currentBalance - parseFloat(amount);
                    
                    // Update wallet balance
                    await connection.execute(
                        `UPDATE user_wallets SET ${column} = ? WHERE user_id = ?`,
                        [newBalance, userId]
                    );
                    
                    // Create wallet transaction
                    await connection.execute(
                        `INSERT INTO wallet_transactions 
                         (id, user_id, wallet_type, amount, type, description, booking_id, balance_before, balance_after)
                         VALUES (?, ?, ?, ?, 'DEBIT', 'Thanh toán dịch vụ', ?, ?, ?)`,
                        [transactionId, userId, method, amount, bookingId, currentBalance, newBalance]
                    );
                }
            }
        }
        
        // 4. Create payment record
        await connection.execute(
            `INSERT INTO payments 
             (id, booking_id, user_id, amount, method, status, transaction_id, completed_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ${paymentStatus === 'COMPLETED' ? 'NOW()' : 'NULL'})`,
            [paymentId, bookingId, userId, amount, method, paymentStatus, transactionId]
        );
        
        // 5. Update booking payment status
        await connection.execute(
            `UPDATE bookings 
             SET payment_method = ?, payment_status = ?, payment_id = ?
             WHERE id = ?`,
            [method, paymentStatus, paymentId, bookingId]
        );
        
        await connection.commit();
        
        res.json({
            success: true,
            message: paymentStatus === 'COMPLETED' ? 'Thanh toán thành công' : 'Đơn hàng đã tạo, thanh toán khi hoàn thành',
            paymentId,
            transactionId,
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

// Get payment by ID
app.get('/api/payments/:id', async (req, res) => {
    try {
        const [payments] = await pool.execute(
            'SELECT * FROM payments WHERE id = ?',
            [req.params.id]
        );
        
        if (payments.length === 0) {
            return res.status(404).json({ success: false, message: 'Payment not found' });
        }
        
        res.json(payments[0]);
    } catch (error) {
        console.error('Get payment error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Get payment by booking ID
app.get('/api/payments/booking/:bookingId', async (req, res) => {
    try {
        const [payments] = await pool.execute(
            'SELECT * FROM payments WHERE booking_id = ?',
            [req.params.bookingId]
        );
        
        if (payments.length === 0) {
            return res.status(404).json({ success: false, message: 'Payment not found' });
        }
        
        res.json(payments[0]);
    } catch (error) {
        console.error('Get payment error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Complete cash payment
app.put('/api/payments/:id/complete', async (req, res) => {
    try {
        await pool.execute(
            `UPDATE payments 
             SET status = 'COMPLETED', completed_at = NOW()
             WHERE id = ?`,
            [req.params.id]
        );
        
        // Update booking payment status
        await pool.execute(
            `UPDATE bookings 
             SET payment_status = 'COMPLETED'
             WHERE payment_id = ?`,
            [req.params.id]
        );
        
        res.json({ success: true, message: 'Payment completed' });
    } catch (error) {
        console.error('Complete payment error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// ============================================
// REVIEW ENDPOINTS
// ============================================

// Create review
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
        if (bookings[0].completed_at) {
            const completedDate = new Date(bookings[0].completed_at);
            const now = new Date();
            const daysDiff = (now - completedDate) / (1000 * 60 * 60 * 24);
            
            if (daysDiff > 7) {
                return res.status(400).json({ 
                    success: false, 
                    message: 'Chỉ có thể đánh giá trong vòng 7 ngày sau khi hoàn thành' 
                });
            }
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

// Get review by booking ID
app.get('/api/reviews/booking/:bookingId', async (req, res) => {
    try {
        const [reviews] = await pool.execute(
            `SELECT r.*, u.name as user_name, w.name as worker_name
             FROM reviews r
             JOIN users u ON r.user_id = u.id
             JOIN workers w ON r.worker_id = w.id
             WHERE r.booking_id = ?`,
            [req.params.bookingId]
        );
        
        if (reviews.length === 0) {
            return res.status(404).json({ success: false, message: 'Review not found' });
        }
        
        res.json(reviews[0]);
    } catch (error) {
        console.error('Get review error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// Get reviews by worker ID
app.get('/api/reviews/worker/:workerId', async (req, res) => {
    try {
        const [reviews] = await pool.execute(
            `SELECT r.*, u.name as user_name, b.service_name
             FROM reviews r
             JOIN users u ON r.user_id = u.id
             JOIN bookings b ON r.booking_id = b.id
             WHERE r.worker_id = ?
             ORDER BY r.created_at DESC`,
            [req.params.workerId]
        );
        
        res.json(reviews);
    } catch (error) {
        console.error('Get worker reviews error:', error);
        res.status(500).json({ success: false, message: error.message });
    }
});

// ============================================
// START SERVER
// ============================================

app.listen(PORT, () => {
    console.log(`🚀 API Server running on http://localhost:${PORT}`);
    console.log(`📊 Database: ${process.env.DB_NAME || 'cleaning_service'}`);
});
