-- =========================================================
-- ACCOUNTS
-- =========================================================

INSERT INTO accounts (
    account_id,
    customer_id,
    status,
    balance
) VALUES
      ('CTA-1001', 'USR-10', 'ACTIVE', 1000000),
      ('CTA-1002', 'USR-10', 'BLOCKED', 800000),
      ('CTA-2001', 'USR-20', 'ACTIVE', 2000000);


-- =========================================================
-- ACCOUNTS_TRANSACTIONS
-- =========================================================

INSERT INTO account_transactions (
    request_reference,
    source_account_id,
    destination_account_id,
    amount
) VALUES
      ('REF-001', 'CTA-1001', 'CTA-2001', 600000),
      ('REF-002', 'CTA-1001', 'CTA-2001', 200000);