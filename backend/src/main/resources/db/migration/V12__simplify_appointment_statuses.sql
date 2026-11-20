UPDATE appointments SET status = 'SCHEDULED' WHERE status = 'CONFIRMED';
UPDATE appointments SET status = 'CANCELLED' WHERE status = 'NO_SHOW';
