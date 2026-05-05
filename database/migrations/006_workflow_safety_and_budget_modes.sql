ALTER TABLE categories
  ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT FALSE AFTER is_default,
  ADD COLUMN budget_mode ENUM('fixed', 'flexible') NOT NULL DEFAULT 'flexible' AFTER is_archived;

ALTER TABLE accounts
  ADD CONSTRAINT uq_accounts_user_name UNIQUE (user_id, name);

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
