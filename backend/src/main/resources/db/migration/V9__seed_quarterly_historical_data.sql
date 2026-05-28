-- Insert historical service request data for previous quarters
INSERT INTO service_requests (customer_name, category, status, attendant_id, created_at, started_at, finished_at, queued_at)
-- February (Q1) data
SELECT 'Aurélio Silva', 'CARD_ISSUES', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 5)::timestamp + TIME '09:15',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 5)::timestamp + TIME '09:15',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 5)::timestamp + TIME '09:38',
       NULL
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Aurélio Silva' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 5)::date)
UNION ALL
SELECT 'Fernanda Costa', 'LOAN_CONTRACTING', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 10)::timestamp + TIME '10:20',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 10)::timestamp + TIME '10:25',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 10)::timestamp + TIME '11:05',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 10)::timestamp + TIME '10:20'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Fernanda Costa' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 10)::date)
UNION ALL
-- March (Q1) data
SELECT 'Gustavo Pereira', 'OTHER_SUBJECTS', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 8)::timestamp + TIME '14:00',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 8)::timestamp + TIME '14:05',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 8)::timestamp + TIME '14:32',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 8)::timestamp + TIME '14:00'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Gustavo Pereira' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 8)::date)
UNION ALL
SELECT 'Helena Monteiro', 'CARD_ISSUES', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 15)::timestamp + TIME '11:30',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 15)::timestamp + TIME '11:30',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 15)::timestamp + TIME '11:50',
       NULL
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Helena Monteiro' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 15)::date)
UNION ALL
-- April (Q1) data
SELECT 'Igor Baptista', 'LOAN_CONTRACTING', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 3)::timestamp + TIME '09:45',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 3)::timestamp + TIME '09:50',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 3)::timestamp + TIME '10:28',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 3)::timestamp + TIME '09:45'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Igor Baptista' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 3)::date)
UNION ALL
SELECT 'Julia Santos', 'OTHER_SUBJECTS', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 20)::timestamp + TIME '15:00',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 20)::timestamp + TIME '15:08',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 20)::timestamp + TIME '15:25',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 20)::timestamp + TIME '15:00'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Julia Santos' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 20)::date)
UNION ALL
-- August (Q3) data
SELECT 'Kevin Alves', 'CARD_ISSUES', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 5)::timestamp + TIME '10:10',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 5)::timestamp + TIME '10:15',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 5)::timestamp + TIME '10:42',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 5)::timestamp + TIME '10:10'
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Kevin Alves' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 5)::date)
UNION ALL
SELECT 'Larissa Gomes', 'LOAN_CONTRACTING', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 12)::timestamp + TIME '13:30',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 12)::timestamp + TIME '13:35',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 12)::timestamp + TIME '14:10',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 12)::timestamp + TIME '13:30'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Larissa Gomes' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 12)::date)
UNION ALL
SELECT 'Marcelo Tavares', 'OTHER_SUBJECTS', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 25)::timestamp + TIME '16:20',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 25)::timestamp + TIME '16:28',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 25)::timestamp + TIME '16:45',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 25)::timestamp + TIME '16:20'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Marcelo Tavares' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 25)::date)
UNION ALL
-- September (Q3) data
SELECT 'Nicole Ribeiro', 'CARD_ISSUES', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 7)::timestamp + TIME '08:50',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 7)::timestamp + TIME '08:55',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 7)::timestamp + TIME '09:15',
       NULL
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Nicole Ribeiro' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 7)::date)
UNION ALL
SELECT 'Otávio Fernandes', 'LOAN_CONTRACTING', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 18)::timestamp + TIME '11:45',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 18)::timestamp + TIME '11:50',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 18)::timestamp + TIME '12:32',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 18)::timestamp + TIME '11:45'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Otávio Fernandes' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 18)::date)
UNION ALL
-- October (Q3) data
SELECT 'Patricia Assis', 'OTHER_SUBJECTS', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 2)::timestamp + TIME '14:15',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 2)::timestamp + TIME '14:22',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 2)::timestamp + TIME '14:38',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 2)::timestamp + TIME '14:15'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Patricia Assis' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 2)::date)
UNION ALL
SELECT 'Quentin Murphy', 'CARD_ISSUES', 'COMPLETED', a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 22)::timestamp + TIME '10:00',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 22)::timestamp + TIME '10:08',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 22)::timestamp + TIME '10:28',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 22)::timestamp + TIME '10:00'
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Quentin Murphy' AND sr.created_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 22)::date);

-- Insert attendant pause data for previous quarters
INSERT INTO attendant_pauses (attendant_id, started_at, finished_at)
SELECT a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 5)::timestamp + TIME '12:00',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 5)::timestamp + TIME '12:15'
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 2, 5)::date
  )
UNION ALL
SELECT a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 15)::timestamp + TIME '15:10',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 15)::timestamp + TIME '15:28'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 3, 15)::date
  )
UNION ALL
SELECT a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 3)::timestamp + TIME '10:30',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 3)::timestamp + TIME '10:45'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 4, 3)::date
  )
UNION ALL
SELECT a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 5)::timestamp + TIME '11:00',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 5)::timestamp + TIME '11:18'
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 8, 5)::date
  )
UNION ALL
SELECT a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 18)::timestamp + TIME '14:00',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 18)::timestamp + TIME '14:12'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 9, 18)::date
  )
UNION ALL
SELECT a.id,
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 2)::timestamp + TIME '16:30',
       make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 2)::timestamp + TIME '16:42'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = make_date(EXTRACT(year FROM CURRENT_DATE)::int, 10, 2)::date
  );
