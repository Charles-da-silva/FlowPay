UPDATE attendants a
SET status = 'BUSY'
WHERE a.status <> 'INACTIVE'
  AND EXISTS (
    SELECT 1
    FROM service_requests sr
    WHERE sr.attendant_id = a.id
      AND sr.status = 'IN_PROGRESS'
  );

UPDATE attendants a
SET status = 'AVAILABLE'
WHERE a.status <> 'INACTIVE'
  AND NOT EXISTS (
    SELECT 1
    FROM service_requests sr
    WHERE sr.attendant_id = a.id
      AND sr.status = 'IN_PROGRESS'
  );
