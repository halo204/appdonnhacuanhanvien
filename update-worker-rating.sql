-- Update worker rating and review count
-- Change these values as needed

-- Update Pham Hoang An's rating
UPDATE workers 
SET rating = 5.0, 
    total_reviews = 10
WHERE name = 'Pham Hoang An';

-- Or update all workers to have realistic ratings
UPDATE workers 
SET rating = 4.5, 
    total_reviews = 5
WHERE status = 'APPROVED';

-- Show updated workers
SELECT id, name, rating, total_reviews, status 
FROM workers 
WHERE status = 'APPROVED'
ORDER BY name;
