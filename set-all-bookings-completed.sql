-- Set all bookings to COMPLETED status for testing review feature
UPDATE bookings 
SET status = 'COMPLETED'
WHERE status != 'COMPLETED';

-- Show updated bookings
SELECT id, service_name, worker_name, status, created_at 
FROM bookings 
ORDER BY created_at DESC 
LIMIT 10;
