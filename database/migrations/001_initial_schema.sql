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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_categories_user_type_name ON categories (user_id, type, name);

CREATE TABLE IF NOT EXISTS transactions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  account_id INT NOT NULL,
  category_id INT NOT NULL,
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
  CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories(id)
);
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date);
CREATE INDEX idx_transactions_user_category_date ON transactions (user_id, category_id, transaction_date);

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
