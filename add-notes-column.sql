-- Add notes column to bookings table if not exists
ALTER TABLE bookings 
ADD COLUMN IF NOT EXISTS notes TEXT AFTER total_price;
