ALTER TABLE attendants
    ADD COLUMN available_since TIMESTAMP WITH TIME ZONE NULL;

UPDATE attendants a
SET available_since = NOW()
WHERE a.status = 'AVAILABLE'
  AND NOT EXISTS (
    SELECT 1
    FROM service_requests sr
    WHERE sr.attendant_id = a.id
      AND sr.status = 'IN_PROGRESS'
  );

ALTER TABLE service_requests
    ADD COLUMN queued_at TIMESTAMP WITH TIME ZONE NULL;

UPDATE service_requests
SET queued_at = created_at
WHERE status = 'WAITING'
  AND queued_at IS NULL;
