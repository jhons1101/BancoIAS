-- =========================================================
-- TABLE: ACCOUNTS
-- =========================================================

CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00
);


-- =========================================================
-- TABLE: ACCOUNTS_TRANSACTIONS
-- =========================================================

CREATE TABLE IF NOT EXISTS account_transactions (
    request_reference VARCHAR(50) PRIMARY KEY,
    source_account_id VARCHAR(50) NOT NULL,
    destination_account_id VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason VARCHAR(255) NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);