# Hướng dẫn setup Phone Authentication & Admin Approval

## Tổng quan luồng đăng ký

1. **Nhân viên đăng ký bằng SĐT** → Nhập OTP
2. **Upload ảnh CMND + ảnh chân dung** → Lưu vào Cloud Storage
3. **Tài khoản ở trạng thái PENDING** → Chờ admin duyệt
4. **Admin duyệt** → Cập nhật status thành APPROVED
5. **Nhân viên nhận thông báo** → Được vào app

## Bước 1: Enable Phone Authentication

### 1.1. Trong Firebase Console

Vào: https://console.firebase.google.com/project/cleaning-service-31385

- Vào **Authentication** → **Sign-in method**
- Bật **Phone**
- Nhấn **Save**

### 1.2. Thêm SHA-1 fingerprint (Quan trọng!)

Phone Auth cần SHA-1 fingerprint của app:

```bash
# Debug SHA-1
cd android
./gradlew signingReport

# Hoặc dùng keytool
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy SHA-1 fingerprint, vào Firebase Console:
- **Project Settings** → **Your apps** → **Android app**
- Nhấn **Add fingerprint**
- Paste SHA-1
- Nhấn **Save**

### 1.3. Tải lại google-services.json

Sau khi thêm SHA-1, tải lại file `google-services.json` mới và thay thế file cũ.

## Bước 2: Setup Cloud Storage

### 2.1. Enable Storage

- Vào **Storage** → **Get started**
- Start in **test mode**
- Location: **asia-southeast1**
- Nhấn **Done**

### 2.2. Cấu hình Storage Rules

Vào **Storage** → **Rules**, paste code:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Workers images
    match /workers/{workerId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == workerId;
    }
  }
}
```

Nhấn **Publish**

## Bước 3: Cấu trúc Database với Worker Status

### 3.1. Worker Profile Structure

```json
{
  "workers": {
    "worker_uid": {
      "id": "worker_uid",
      "name": "Nguyễn Văn A",
      "phone": "+84901234567",
      "email": "",
      "status": "PENDING",
      "idCardImageUrl": "https://storage.googleapis.com/...",
      "portraitImageUrl": "https://storage.googleapis.com/...",
      "completedJobs": 0,
      "averageRating": 0.0,
      "totalEarnings": 0,
      "todayEarnings": 0,
      "isOnline": false,
      "createdAt": 1234567890000
    }
  }
}
```

### 3.2. Worker Status Values

- **PENDING**: Chờ admin duyệt (mặc định khi đăng ký)
- **APPROVED**: Đã được duyệt, có thể vào app
- **REJECTED**: Bị từ chối

## Bước 4: Admin Panel (Web)

Tạo admin panel đơn giản để duyệt nhân viên:

### 4.1. Tạo file HTML

Tạo file `admin-panel.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Admin Panel - Worker Approval</title>
    <script src="https://www.gstatic.com/firebasejs/10.7.0/firebase-app-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/10.7.0/firebase-auth-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/10.7.0/firebase-database-compat.js"></script>
    <style>
        body { font-family: Arial; padding: 20px; }
        .worker-card { 
            border: 1px solid #ddd; 
            padding: 15px; 
            margin: 10px 0; 
            border-radius: 8px;
        }
        .worker-card img { 
            max-width: 200px; 
            margin: 10px; 
            border-radius: 4px;
        }
        .status-pending { background: #fff3cd; }
        .status-approved { background: #d4edda; }
        .status-rejected { background: #f8d7da; }
        button { 
            padding: 10px 20px; 
            margin: 5px; 
            cursor: pointer;
            border: none;
            border-radius: 4px;
        }
        .btn-approve { background: #28a745; color: white; }
        .btn-reject { background: #dc3545; color: white; }
    </style>
</head>
<body>
    <h1>Worker Approval Panel</h1>
    <div id="login-section">
        <h3>Admin Login</h3>
        <input type="email" id="admin-email" placeholder="Email">
        <input type="password" id="admin-password" placeholder="Password">
        <button onclick="adminLogin()">Login</button>
    </div>
    
    <div id="workers-section" style="display:none;">
        <h3>Pending Workers</h3>
        <div id="workers-list"></div>
    </div>

    <script>
        // Firebase config
        const firebaseConfig = {
            apiKey: "AIzaSyDv1hFGUZkj_LCqT6S6e4dekf5e4DB-vqU",
            authDomain: "cleaning-service-31385.firebaseapp.com",
            databaseURL: "https://cleaning-service-31385-default-rtdb.asia-southeast1.firebasedatabase.app",
            projectId: "cleaning-service-31385",
            storageBucket: "cleaning-service-31385.firebasestorage.app"
        };
        
        firebase.initializeApp(firebaseConfig);
        const auth = firebase.auth();
        const database = firebase.database();
        
        function adminLogin() {
            const email = document.getElementById('admin-email').value;
            const password = document.getElementById('admin-password').value;
            
            auth.signInWithEmailAndPassword(email, password)
                .then(() => {
                    document.getElementById('login-section').style.display = 'none';
                    document.getElementById('workers-section').style.display = 'block';
                    loadWorkers();
                })
                .catch(error => alert('Login failed: ' + error.message));
        }
        
        function loadWorkers() {
            database.ref('workers').on('value', (snapshot) => {
                const workersList = document.getElementById('workers-list');
                workersList.innerHTML = '';
                
                snapshot.forEach((childSnapshot) => {
                    const worker = childSnapshot.val();
                    const workerId = childSnapshot.key;
                    
                    if (worker.status === 'PENDING') {
                        const card = createWorkerCard(worker, workerId);
                        workersList.appendChild(card);
                    }
                });
            });
        }
        
        function createWorkerCard(worker, workerId) {
            const card = document.createElement('div');
            card.className = 'worker-card status-' + worker.status.toLowerCase();
            card.innerHTML = `
                <h3>${worker.name}</h3>
                <p><strong>Phone:</strong> ${worker.phone}</p>
                <p><strong>Status:</strong> ${worker.status}</p>
                <p><strong>Registered:</strong> ${new Date(worker.createdAt).toLocaleString()}</p>
                <div>
                    <img src="${worker.idCardImageUrl}" alt="ID Card">
                    <img src="${worker.portraitImageUrl}" alt="Portrait">
                </div>
                <button class="btn-approve" onclick="approveWorker('${workerId}')">Approve</button>
                <button class="btn-reject" onclick="rejectWorker('${workerId}')">Reject</button>
            `;
            return card;
        }
        
        function approveWorker(workerId) {
            if (confirm('Approve this worker?')) {
                database.ref('workers/' + workerId).update({
                    status: 'APPROVED'
                }).then(() => {
                    alert('Worker approved!');
                });
            }
        }
        
        function rejectWorker(workerId) {
            if (confirm('Reject this worker?')) {
                database.ref('workers/' + workerId).update({
                    status: 'REJECTED'
                }).then(() => {
                    alert('Worker rejected!');
                });
            }
        }
    </script>
</body>
</html>
```

### 4.2. Tạo Admin Account

Trong Firebase Console:
- Vào **Authentication** → **Users**
- Nhấn **Add user**
- Email: `admin@cleanservice.com`
- Password: `admin123456`
- Nhấn **Add user**

### 4.3. Sử dụng Admin Panel

1. Mở file `admin-panel.html` trong browser
2. Login với email/password admin
3. Xem danh sách workers đang chờ duyệt
4. Nhấn **Approve** hoặc **Reject**

## Bước 5: Test Flow

### 5.1. Test đăng ký Worker

1. Mở Worker App
2. Nhấn "Đăng ký bằng số điện thoại"
3. Nhập SĐT: `0901234567`
4. Nhập OTP (nhận qua SMS)
5. Nhập họ tên
6. Upload ảnh CMND
7. Upload ảnh chân dung
8. Nhấn "Hoàn tất đăng ký"
9. Màn hình hiển thị "Đang chờ phê duyệt"

### 5.2. Test Admin Approval

1. Mở Admin Panel
2. Login với admin account
3. Thấy worker mới trong danh sách
4. Xem ảnh CMND và chân dung
5. Nhấn "Approve"

### 5.3. Test Worker Login

1. Worker App tự động cập nhật status
2. Chuyển sang màn hình Home
3. Worker có thể bắt đầu nhận đơn

## Bước 6: Security Rules

### 6.1. Realtime Database Rules

```json
{
  "rules": {
    "workers": {
      "$workerId": {
        ".read": "auth != null",
        ".write": "auth != null && (
          auth.uid == $workerId ||
          root.child('admins').child(auth.uid).exists()
        )"
      }
    },
    "admins": {
      ".read": "auth != null && root.child('admins').child(auth.uid).exists()",
      ".write": false
    }
  }
}
```

### 6.2. Thêm Admin UID vào Database

Trong Realtime Database, tạo node `admins`:

```json
{
  "admins": {
    "admin_uid_here": true
  }
}
```

## Troubleshooting

### Phone Auth không hoạt động

1. Kiểm tra SHA-1 fingerprint đã thêm chưa
2. Tải lại `google-services.json` mới
3. Clean và rebuild app: `./gradlew clean assembleDebug`
4. Kiểm tra Phone Auth đã enable trong Firebase Console

### Upload ảnh thất bại

1. Kiểm tra Storage đã enable chưa
2. Kiểm tra Storage Rules
3. Kiểm tra internet connection

### Worker không nhận được thông báo khi được duyệt

1. App đang lắng nghe real-time updates từ Database
2. Kiểm tra worker có đang mở app không
3. Kiểm tra status trong Database đã update chưa

## Next Steps

1. **Push Notifications**: Thêm FCM để gửi thông báo khi được duyệt
2. **Cloud Functions**: Tự động gửi email/SMS khi status thay đổi
3. **Admin Dashboard**: Tạo admin panel chuyên nghiệp hơn với React/Vue
4. **Analytics**: Theo dõi số lượng đăng ký, approval rate, etc.
