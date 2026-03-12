-- Xóa worker với email trùng
USE cleaning_service;

-- Kiểm tra worker với email này
SELECT id, name, email, status FROM workers WHERE email = 'goldendog150205@gmail.com';

-- Xóa worker (nếu muốn)
DELETE FROM workers WHERE email = 'goldendog150205@gmail.com';

-- Kiểm tra lại
SELECT id, name, email, status FROM workers WHERE email = 'goldendog150205@gmail.com';
