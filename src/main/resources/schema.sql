-- =========================================================
-- TABLE: ACCOUNTS
-- =========================================================

CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,

    CONSTRAINT chk_account_status
    CHECK (status IN ('ACTIVE', 'BLOCKED')),

    CONSTRAINT chk_account_balance
    CHECK (balance >= 0)
);


-- =========================================================
-- TABLE: ACCOUNTS_TRANSACTIONS
-- =========================================================

CREATE TABLE IF NOT EXISTS account_transactions (
    request_reference VARCHAR(50) PRIMARY KEY,
    source_account_id VARCHAR(50) NOT NULL,
    destination_account_id VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaction_source_account
    FOREIGN KEY (source_account_id)
    REFERENCES accounts(account_id),

    CONSTRAINT fk_transaction_destination_account
    FOREIGN KEY (destination_account_id)
    REFERENCES accounts(account_id),

    CONSTRAINT chk_transaction_amount
    CHECK (amount > 0)
);