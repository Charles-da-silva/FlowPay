INSERT INTO attendants (name, status, max_simultaneous_customers)
VALUES
    ('Ana Cardoso', 'AVAILABLE', 3),
    ('Bruno Lima', 'AVAILABLE', 3),
    ('Carla Souza', 'AVAILABLE', 3);

INSERT INTO attendant_categories (attendant_id, category)
VALUES
    (1, 'CARD_ISSUES'),
    (1, 'OTHER_SUBJECTS'),
    (2, 'LOAN_CONTRACTING'),
    (2, 'OTHER_SUBJECTS'),
    (3, 'CARD_ISSUES'),
    (3, 'LOAN_CONTRACTING'),
    (3, 'OTHER_SUBJECTS');
