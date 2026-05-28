-- Insert historical service request data for January 2026 (Q1 start)
INSERT INTO service_requests (customer_name, category, status, attendant_id, created_at, started_at, finished_at, queued_at)
-- January data
SELECT 'Rafael Mendes', 'CARD_ISSUES', 'COMPLETED', a.id,
       make_date(2026, 1, 5)::timestamp + TIME '09:15',
       make_date(2026, 1, 5)::timestamp + TIME '09:15',
       make_date(2026, 1, 5)::timestamp + TIME '09:38',
       NULL
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Rafael Mendes' AND sr.created_at::date = make_date(2026, 1, 5)::date)
UNION ALL
SELECT 'Sandra Rocha', 'LOAN_CONTRACTING', 'COMPLETED', a.id,
       make_date(2026, 1, 10)::timestamp + TIME '10:20',
       make_date(2026, 1, 10)::timestamp + TIME '10:25',
       make_date(2026, 1, 10)::timestamp + TIME '11:05',
       make_date(2026, 1, 10)::timestamp + TIME '10:20'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Sandra Rocha' AND sr.created_at::date = make_date(2026, 1, 10)::date)
UNION ALL
SELECT 'Tiago Ferreira', 'OTHER_SUBJECTS', 'COMPLETED', a.id,
       make_date(2026, 1, 15)::timestamp + TIME '14:00',
       make_date(2026, 1, 15)::timestamp + TIME '14:05',
       make_date(2026, 1, 15)::timestamp + TIME '14:32',
       make_date(2026, 1, 15)::timestamp + TIME '14:00'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Tiago Ferreira' AND sr.created_at::date = make_date(2026, 1, 15)::date)
UNION ALL
SELECT 'Ursula Campos', 'CARD_ISSUES', 'COMPLETED', a.id,
       make_date(2026, 1, 20)::timestamp + TIME '11:30',
       make_date(2026, 1, 20)::timestamp + TIME '11:30',
       make_date(2026, 1, 20)::timestamp + TIME '11:50',
       NULL
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Ursula Campos' AND sr.created_at::date = make_date(2026, 1, 20)::date)
UNION ALL
SELECT 'Victor Santos', 'LOAN_CONTRACTING', 'COMPLETED', a.id,
       make_date(2026, 1, 25)::timestamp + TIME '09:45',
       make_date(2026, 1, 25)::timestamp + TIME '09:50',
       make_date(2026, 1, 25)::timestamp + TIME '10:28',
       make_date(2026, 1, 25)::timestamp + TIME '09:45'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Victor Santos' AND sr.created_at::date = make_date(2026, 1, 25)::date)
UNION ALL
SELECT 'Wanda Silva', 'OTHER_SUBJECTS', 'COMPLETED', a.id,
       make_date(2026, 1, 28)::timestamp + TIME '15:00',
       make_date(2026, 1, 28)::timestamp + TIME '15:08',
       make_date(2026, 1, 28)::timestamp + TIME '15:25',
       make_date(2026, 1, 28)::timestamp + TIME '15:00'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Wanda Silva' AND sr.created_at::date = make_date(2026, 1, 28)::date);

-- Insert attendant pause data for January 2026
INSERT INTO attendant_pauses (attendant_id, started_at, finished_at)
SELECT a.id,
       make_date(2026, 1, 5)::timestamp + TIME '12:00',
       make_date(2026, 1, 5)::timestamp + TIME '12:15'
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(2026, 1, 5)::date
  )
UNION ALL
SELECT a.id,
       make_date(2026, 1, 10)::timestamp + TIME '15:10',
       make_date(2026, 1, 10)::timestamp + TIME '15:28'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(2026, 1, 10)::date
  )
UNION ALL
SELECT a.id,
       make_date(2026, 1, 15)::timestamp + TIME '10:30',
       make_date(2026, 1, 15)::timestamp + TIME '10:45'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(2026, 1, 15)::date
  )
UNION ALL
SELECT a.id,
       make_date(2026, 1, 20)::timestamp + TIME '11:00',
       make_date(2026, 1, 20)::timestamp + TIME '11:18'
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(2026, 1, 20)::date
  )
UNION ALL
SELECT a.id,
       make_date(2026, 1, 25)::timestamp + TIME '14:00',
       make_date(2026, 1, 25)::timestamp + TIME '14:12'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(2026, 1, 25)::date
  )
UNION ALL
SELECT a.id,
       make_date(2026, 1, 28)::timestamp + TIME '16:30',
       make_date(2026, 1, 28)::timestamp + TIME '16:42'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(2026, 1, 28)::date
  );
