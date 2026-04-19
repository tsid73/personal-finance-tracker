CREATE DATABASE IF NOT EXISTS personal_finance;
USE personal_finance;

CREATE TABLE IF NOT EXISTS users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  name VARCHAR(80) NOT NULL,
  type ENUM('cash', 'bank', 'wallet', 'credit') NOT NULL DEFAULT 'bank',
  balance DECIMAL(12, 2) NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_accounts_user_name UNIQUE (user_id, name),
  CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_accounts_user_type ON accounts (user_id, type);

CREATE TABLE IF NOT EXISTS categories (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NULL,
  name VARCHAR(80) NOT NULL,
  type ENUM('income', 'expense') NOT NULL DEFAULT 'expense',
  color VARCHAR(20) NOT NULL DEFAULT '#0f766e',
  icon VARCHAR(40) NOT NULL DEFAULT 'wallet',
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  is_archived BOOLEAN NOT NULL DEFAULT FALSE,
  budget_mode ENUM('fixed', 'flexible') NOT NULL DEFAULT 'flexible',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_categories_user_type_name ON categories (user_id, type, name);

CREATE TABLE IF NOT EXISTS activity_logs (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id INT NULL,
  action VARCHAR(40) NOT NULL,
  title VARCHAR(160) NOT NULL,
  note VARCHAR(300) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_activity_logs_user_created ON activity_logs (user_id, created_at DESC);

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

CREATE TABLE IF NOT EXISTS transactions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  account_id INT NOT NULL,
  category_id INT NOT NULL,
  recurring_transaction_id INT NULL,
  kind ENUM('income', 'expense') NOT NULL,
  title VARCHAR(140) NOT NULL,
  notes TEXT NULL,
  merchant VARCHAR(120) NULL,
  amount DECIMAL(12, 2) NOT NULL,
  transaction_date DATE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT fk_transactions_recurring_transaction FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transactions(id) ON DELETE SET NULL
);
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date);
CREATE INDEX idx_transactions_user_category_date ON transactions (user_id, category_id, transaction_date);
CREATE INDEX idx_transactions_recurring_date ON transactions (recurring_transaction_id, transaction_date);

CREATE TABLE IF NOT EXISTS budgets (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  category_id INT NOT NULL,
  month TINYINT NOT NULL,
  year SMALLINT NOT NULL,
  allocated_amount DECIMAL(12, 2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT uq_budget_period UNIQUE (user_id, category_id, month, year)
);
CREATE INDEX idx_budgets_user_period ON budgets (user_id, year, month);

CREATE TABLE IF NOT EXISTS monthly_budget_targets (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  month TINYINT NOT NULL,
  year SMALLINT NOT NULL,
  total_budget DECIMAL(12, 2) NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_monthly_budget_targets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT uq_monthly_budget_target UNIQUE (user_id, month, year)
);
CREATE INDEX idx_monthly_budget_targets_user_period ON monthly_budget_targets (user_id, year, month);

INSERT INTO users (id, full_name, email)
VALUES (1, 'Demo User', 'demo@example.com')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO accounts (id, user_id, name, type, balance)
VALUES
  (1, 1, 'Bank', 'bank', 42850.00),
  (2, 1, 'Cash', 'cash', 2400.00),
  (3, 1, 'Credit Card', 'credit', -3800.00),
  (4, 1, 'UPI', 'bank', 0.00),
  (5, 1, 'UPI-Lite', 'bank', 0.00),
  (6, 1, 'NEFT', 'bank', 0.00)
ON DUPLICATE KEY UPDATE name = VALUES(name), balance = VALUES(balance);

INSERT INTO categories (id, user_id, name, type, color, icon, is_default)
VALUES
  (1, NULL, 'Groceries', 'expense', '#0f766e', 'shopping-bag', TRUE),
  (2, NULL, 'Rent', 'expense', '#b45309', 'home', TRUE),
  (3, NULL, 'Utilities', 'expense', '#2563eb', 'bolt', TRUE),
  (4, NULL, 'Transport', 'expense', '#7c3aed', 'car', TRUE),
  (5, NULL, 'Dining', 'expense', '#dc2626', 'utensils', TRUE),
  (6, NULL, 'Healthcare', 'expense', '#db2777', 'heart-pulse', TRUE),
  (7, NULL, 'Entertainment', 'expense', '#0891b2', 'film', TRUE),
  (8, NULL, 'Shopping', 'expense', '#ea580c', 'shirt', TRUE),
  (9, NULL, 'Salary', 'income', '#15803d', 'briefcase', TRUE),
  (10, NULL, 'Freelance', 'income', '#4338ca', 'laptop', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), color = VALUES(color);

INSERT INTO transactions (user_id, account_id, category_id, kind, title, notes, merchant, amount, transaction_date)
VALUES
  (1, 1, 9, 'income', 'April salary', 'Monthly salary credit', 'Employer Inc.', 85000.00, '2026-04-01'),
  (1, 1, 2, 'expense', 'House rent', 'Monthly apartment rent', 'Landlord', 25000.00, '2026-04-03'),
  (1, 1, 1, 'expense', 'Weekly groceries', 'Milk, vegetables, and staples', 'Fresh Mart', 3200.00, '2026-04-05'),
  (1, 3, 5, 'expense', 'Team dinner', 'Dinner with friends', 'Spice Table', 1850.00, '2026-04-06'),
  (1, 2, 4, 'expense', 'Metro recharge', 'Monthly travel pass', 'City Metro', 1200.00, '2026-04-07');

INSERT INTO budgets (user_id, category_id, month, year, allocated_amount)
VALUES
  (1, 1, 4, 2026, 12000.00),
  (1, 2, 4, 2026, 25000.00),
  (1, 3, 4, 2026, 5000.00),
  (1, 4, 4, 2026, 3500.00),
  (1, 5, 4, 2026, 6000.00),
  (1, 7, 4, 2026, 4000.00)
ON DUPLICATE KEY UPDATE allocated_amount = VALUES(allocated_amount);

INSERT IGNORE INTO monthly_budget_targets (user_id, month, year, total_budget)
VALUES (1, 4, 2026, 130000.00);

