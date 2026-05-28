INSERT INTO service_requests (customer_name, category, status, attendant_id, created_at, started_at, finished_at, queued_at)
SELECT 'Mariana Oliveira', 'CARD_ISSUES', 'COMPLETED', a.id,
       CURRENT_DATE - INTERVAL '7 days' + TIME '09:05',
       CURRENT_DATE - INTERVAL '7 days' + TIME '09:05',
       CURRENT_DATE - INTERVAL '7 days' + TIME '09:22',
       NULL
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Mariana Oliveira')
UNION ALL
SELECT 'Paulo Mendes', 'CARD_ISSUES', 'COMPLETED', a.id,
       CURRENT_DATE - INTERVAL '7 days' + TIME '10:12',
       CURRENT_DATE - INTERVAL '7 days' + TIME '10:20',
       CURRENT_DATE - INTERVAL '7 days' + TIME '10:44',
       CURRENT_DATE - INTERVAL '7 days' + TIME '10:12'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Paulo Mendes')
UNION ALL
SELECT 'Renata Costa', 'LOAN_CONTRACTING', 'COMPLETED', a.id,
       CURRENT_DATE - INTERVAL '6 days' + TIME '14:03',
       CURRENT_DATE - INTERVAL '6 days' + TIME '14:03',
       CURRENT_DATE - INTERVAL '6 days' + TIME '14:31',
       NULL
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Renata Costa')
UNION ALL
SELECT 'Felipe Rocha', 'OTHER_SUBJECTS', 'COMPLETED', a.id,
       CURRENT_DATE - INTERVAL '6 days' + TIME '16:45',
       CURRENT_DATE - INTERVAL '6 days' + TIME '16:52',
       CURRENT_DATE - INTERVAL '6 days' + TIME '17:08',
       CURRENT_DATE - INTERVAL '6 days' + TIME '16:45'
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Felipe Rocha')
UNION ALL
SELECT 'Camila Duarte', 'LOAN_CONTRACTING', 'COMPLETED', a.id,
       CURRENT_DATE - INTERVAL '3 days' + TIME '08:50',
       CURRENT_DATE - INTERVAL '3 days' + TIME '08:50',
       CURRENT_DATE - INTERVAL '3 days' + TIME '09:19',
       NULL
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Camila Duarte')
UNION ALL
SELECT 'Lucas Martins', 'OTHER_SUBJECTS', 'COMPLETED', a.id,
       CURRENT_DATE - INTERVAL '3 days' + TIME '11:10',
       CURRENT_DATE - INTERVAL '3 days' + TIME '11:24',
       CURRENT_DATE - INTERVAL '3 days' + TIME '11:47',
       CURRENT_DATE - INTERVAL '3 days' + TIME '11:10'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Lucas Martins')
UNION ALL
SELECT 'Bianca Ramos', 'CARD_ISSUES', 'COMPLETED', a.id,
       CURRENT_DATE - INTERVAL '1 day' + TIME '13:35',
       CURRENT_DATE - INTERVAL '1 day' + TIME '13:35',
       CURRENT_DATE - INTERVAL '1 day' + TIME '13:59',
       NULL
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Bianca Ramos')
UNION ALL
SELECT 'Eduardo Nunes', 'OTHER_SUBJECTS', 'COMPLETED', a.id,
       CURRENT_DATE - INTERVAL '1 day' + TIME '15:20',
       CURRENT_DATE - INTERVAL '1 day' + TIME '15:28',
       CURRENT_DATE - INTERVAL '1 day' + TIME '15:51',
       CURRENT_DATE - INTERVAL '1 day' + TIME '15:20'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (SELECT 1 FROM service_requests sr WHERE sr.customer_name = 'Eduardo Nunes');

INSERT INTO attendant_pauses (attendant_id, started_at, finished_at)
SELECT a.id,
       CURRENT_DATE - INTERVAL '7 days' + TIME '12:00',
       CURRENT_DATE - INTERVAL '7 days' + TIME '12:18'
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = (CURRENT_DATE - INTERVAL '7 days')::date
  )
UNION ALL
SELECT a.id,
       CURRENT_DATE - INTERVAL '6 days' + TIME '15:10',
       CURRENT_DATE - INTERVAL '6 days' + TIME '15:32'
FROM attendants a
WHERE a.badge = 'AG0002'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = (CURRENT_DATE - INTERVAL '6 days')::date
  )
UNION ALL
SELECT a.id,
       CURRENT_DATE - INTERVAL '3 days' + TIME '10:00',
       CURRENT_DATE - INTERVAL '3 days' + TIME '10:15'
FROM attendants a
WHERE a.badge = 'AG0003'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = (CURRENT_DATE - INTERVAL '3 days')::date
  )
UNION ALL
SELECT a.id,
       CURRENT_DATE - INTERVAL '1 day' + TIME '17:05',
       CURRENT_DATE - INTERVAL '1 day' + TIME '17:20'
FROM attendants a
WHERE a.badge = 'AG0001'
  AND NOT EXISTS (
      SELECT 1 FROM attendant_pauses ap
      WHERE ap.attendant_id = a.id
        AND ap.started_at::date = (CURRENT_DATE - INTERVAL '1 day')::date
  );
