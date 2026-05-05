CREATE TABLE IF NOT EXISTS recurring_transactions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  account_id INT NOT NULL,
  category_id INT NOT NULL,
  kind ENUM('income', 'expense') NOT NULL,
  title VARCHAR(140) NOT NULL,
  notes TEXT NULL,
  merchant VARCHAR(120) NULL,
  amount DECIMAL(12, 2) NOT NULL,
  frequency ENUM('monthly') NOT NULL DEFAULT 'monthly',
  day_of_month TINYINT NOT NULL,
  start_date DATE NOT NULL,
  next_due_date DATE NOT NULL,
  auto_create BOOLEAN NOT NULL DEFAULT TRUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_recurring_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_recurring_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  CONSTRAINT fk_recurring_transactions_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_recurring_transactions_user_due ON recurring_transactions (user_id, is_active, next_due_date);

ALTER TABLE transactions
  ADD COLUMN recurring_transaction_id INT NULL AFTER category_id,
  ADD CONSTRAINT fk_transactions_recurring_transaction
    FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transactions(id) ON DELETE SET NULL;

CREATE INDEX idx_transactions_recurring_date ON transactions (recurring_transaction_id, transaction_date);
