WITH ranked_attendants AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM attendants
    WHERE status <> 'INACTIVE'
)
INSERT INTO service_requests (customer_name, category, status, attendant_id, created_at, started_at, finished_at, queued_at)
SELECT item.customer_name,
       item.category,
       'COMPLETED',
       ra.id,
       item.created_at,
       item.started_at,
       item.finished_at,
       item.queued_at
FROM (
    VALUES
        ('Helena Farias', 'CARD_ISSUES', 1, CURRENT_DATE - INTERVAL '12 days' + TIME '09:15', CURRENT_DATE - INTERVAL '12 days' + TIME '09:15', CURRENT_DATE - INTERVAL '12 days' + TIME '09:33', NULL),
        ('Rafael Teixeira', 'LOAN_CONTRACTING', 2, CURRENT_DATE - INTERVAL '12 days' + TIME '10:40', CURRENT_DATE - INTERVAL '12 days' + TIME '10:52', CURRENT_DATE - INTERVAL '12 days' + TIME '11:18', CURRENT_DATE - INTERVAL '12 days' + TIME '10:40'),
        ('Sofia Almeida', 'OTHER_SUBJECTS', 3, CURRENT_DATE - INTERVAL '10 days' + TIME '15:05', CURRENT_DATE - INTERVAL '10 days' + TIME '15:05', CURRENT_DATE - INTERVAL '10 days' + TIME '15:21', NULL),
        ('Daniel Pereira', 'CARD_ISSUES', 1, CURRENT_DATE - INTERVAL '8 days' + TIME '13:20', CURRENT_DATE - INTERVAL '8 days' + TIME '13:31', CURRENT_DATE - INTERVAL '8 days' + TIME '13:58', CURRENT_DATE - INTERVAL '8 days' + TIME '13:20'),
        ('Larissa Gomes', 'LOAN_CONTRACTING', 2, CURRENT_DATE - INTERVAL '5 days' + TIME '08:25', CURRENT_DATE - INTERVAL '5 days' + TIME '08:25', CURRENT_DATE - INTERVAL '5 days' + TIME '08:49', NULL),
        ('Thiago Batista', 'OTHER_SUBJECTS', 3, CURRENT_DATE - INTERVAL '5 days' + TIME '16:10', CURRENT_DATE - INTERVAL '5 days' + TIME '16:24', CURRENT_DATE - INTERVAL '5 days' + TIME '16:45', CURRENT_DATE - INTERVAL '5 days' + TIME '16:10'),
        ('Priscila Neves', 'CARD_ISSUES', 1, CURRENT_DATE - INTERVAL '2 days' + TIME '11:35', CURRENT_DATE - INTERVAL '2 days' + TIME '11:35', CURRENT_DATE - INTERVAL '2 days' + TIME '11:57', NULL),
        ('Gustavo Freitas', 'LOAN_CONTRACTING', 2, CURRENT_DATE - INTERVAL '2 days' + TIME '17:05', CURRENT_DATE - INTERVAL '2 days' + TIME '17:18', CURRENT_DATE - INTERVAL '2 days' + TIME '17:39', CURRENT_DATE - INTERVAL '2 days' + TIME '17:05')
) AS item(customer_name, category, attendant_rank, created_at, started_at, finished_at, queued_at)
JOIN ranked_attendants ra ON ra.rn = item.attendant_rank
WHERE NOT EXISTS (
    SELECT 1
    FROM service_requests sr
    WHERE sr.customer_name = item.customer_name
);

WITH ranked_attendants AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM attendants
    WHERE status <> 'INACTIVE'
)
INSERT INTO attendant_pauses (attendant_id, started_at, finished_at)
SELECT ra.id,
       item.started_at,
       item.finished_at
FROM (
    VALUES
        (1, CURRENT_DATE - INTERVAL '12 days' + TIME '12:00', CURRENT_DATE - INTERVAL '12 days' + TIME '12:16'),
        (2, CURRENT_DATE - INTERVAL '10 days' + TIME '12:30', CURRENT_DATE - INTERVAL '10 days' + TIME '12:52'),
        (3, CURRENT_DATE - INTERVAL '5 days' + TIME '14:00', CURRENT_DATE - INTERVAL '5 days' + TIME '14:20'),
        (1, CURRENT_DATE - INTERVAL '2 days' + TIME '15:10', CURRENT_DATE - INTERVAL '2 days' + TIME '15:25')
) AS item(attendant_rank, started_at, finished_at)
JOIN ranked_attendants ra ON ra.rn = item.attendant_rank
WHERE NOT EXISTS (
    SELECT 1
    FROM attendant_pauses ap
    WHERE ap.attendant_id = ra.id
      AND ap.started_at = item.started_at
);
