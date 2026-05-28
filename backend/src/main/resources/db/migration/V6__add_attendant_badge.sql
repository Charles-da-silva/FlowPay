ALTER TABLE attendants
    ADD COLUMN badge VARCHAR(20) NULL;

UPDATE attendants
SET badge = 'AG' || LPAD(id::TEXT, 4, '0')
WHERE badge IS NULL;

ALTER TABLE attendants
    ALTER COLUMN badge SET NOT NULL;

ALTER TABLE attendants
    ADD CONSTRAINT uk_attendants_badge UNIQUE (badge);
