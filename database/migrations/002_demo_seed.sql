INSERT INTO users (id, full_name, email)
VALUES (1, 'Demo User', 'demo@example.com')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO accounts (id, user_id, name, type, balance)
VALUES
  (1, 1, 'Bank', 'bank', 42850.00),
  (2, 1, 'Cash', 'cash', 2400.00),
  (3, 1, 'Credit Card', 'credit', -3800.00)
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
ON DUPLICATE KEY UPDATE name = VALUES(name), color = VALUES(color), icon = VALUES(icon), is_default = VALUES(is_default);

INSERT IGNORE INTO transactions (user_id, account_id, category_id, kind, title, notes, merchant, amount, transaction_date)
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
