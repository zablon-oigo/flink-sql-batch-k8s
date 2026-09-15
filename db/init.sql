CREATE USER demo WITH PASSWORD 'demo';

CREATE DATABASE transactions;

GRANT ALL PRIVILEGES ON DATABASE transactions TO demo;

\connect transactions

CREATE TABLE transactions (
    transaction_id BIGINT PRIMARY KEY,
    product_id     BIGINT NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL,
    created_tst    TIMESTAMP NOT NULL
);

GRANT SELECT ON transactions TO demo;

-- Transactions from two days ago
INSERT INTO transactions (transaction_id, product_id, amount, created_tst) VALUES
    (1,  1,  20.00, CURRENT_DATE - INTERVAL '2 days' + TIME '09:15:00'),
    (2,  1,  20.00, CURRENT_DATE - INTERVAL '2 days' + TIME '11:30:00'),
    (3,  2, 120.00, CURRENT_DATE - INTERVAL '2 days' + TIME '16:45:00');

-- Transactions from yesterday.
-- These are the records that should be processed by the batch job.
INSERT INTO transactions (transaction_id, product_id, amount, created_tst) VALUES
    (4,  1,  20.00, CURRENT_DATE - INTERVAL '1 day' + TIME '08:10:00'),
    (5,  1,  20.00, CURRENT_DATE - INTERVAL '1 day' + TIME '10:20:00'),
    (6,  2, 120.00, CURRENT_DATE - INTERVAL '1 day' + TIME '12:30:00'),
    (7,  2, 120.00, CURRENT_DATE - INTERVAL '1 day' + TIME '14:40:00'),
    (8,  2, 120.00, CURRENT_DATE - INTERVAL '1 day' + TIME '17:50:00'),
    (9,  3,  45.00, CURRENT_DATE - INTERVAL '1 day' + TIME '21:15:00');

-- Transactions from today
INSERT INTO transactions (transaction_id, product_id, amount, created_tst) VALUES
    (10, 1,  20.00, CURRENT_DATE + TIME '08:00:00'),
    (11, 2, 120.00, CURRENT_DATE + TIME '11:00:00'),
    (12, 3,  45.00, CURRENT_DATE + TIME '15:00:00');