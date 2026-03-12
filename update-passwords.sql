-- ============================================
-- UPDATE PASSWORDS WITH REAL BCRYPT HASHES
-- Password: password123
-- ============================================

USE cleaning_service;

-- Update users (customers) passwords
UPDATE users SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'customer1@example.com';
UPDATE users SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'customer2@example.com';

-- Update workers passwords
UPDATE workers SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'worker1@example.com';
UPDATE workers SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'worker2@example.com';
UPDATE workers SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'worker3@example.com';
UPDATE workers SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'worker4@example.com';

SELECT 'Passwords updated successfully!' as message;
