-- Captured from live Docker MySQL backup: backups/personal_finance-20260430-225612-full.sql.gz
-- Generated on 2026-04-30 to preserve latest local entries without depending on live primary keys.

SET @demo_user_id := 1;

SET @bank_account_id := (
  SELECT id FROM accounts WHERE user_id = @demo_user_id AND name = 'Bank' LIMIT 1
);
SET @cash_account_id := (
  SELECT id FROM accounts WHERE user_id = @demo_user_id AND name = 'Cash' LIMIT 1
);
SET @upi_account_id := (
  SELECT id FROM accounts WHERE user_id = @demo_user_id AND name = 'UPI' LIMIT 1
);

SET @utilities_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Utilities'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);
SET @groceries_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Groceries'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);
SET @credit_card_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Credit Card'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);
SET @emi_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'EMI'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);
SET @misc_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Misc'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);
SET @food_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Food'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);
SET @investment_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Investment'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);

INSERT INTO budgets (user_id, category_id, month, year, allocated_amount)
VALUES
  (@demo_user_id, @credit_card_category_id, 4, 2026, 15000.00),
  (@demo_user_id, @emi_category_id, 4, 2026, 31000.00),
  (@demo_user_id, @food_category_id, 4, 2026, 3000.00),
  (@demo_user_id, @investment_category_id, 4, 2026, 45000.00),
  (@demo_user_id, @misc_category_id, 4, 2026, 5000.00),
  (@demo_user_id, @utilities_category_id, 4, 2026, 5000.00)
ON DUPLICATE KEY UPDATE allocated_amount = VALUES(allocated_amount);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @utilities_category_id, NULL, 'expense', 'Apay', NULL, 'amazon', 1500.00, '2026-04-03', '2026-04-09 10:43:43', '2026-04-09 10:43:43'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @utilities_category_id
    AND kind = 'expense' AND title = 'Apay' AND amount = 1500.00 AND transaction_date = '2026-04-03'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @misc_category_id, NULL, 'expense', 'Tank Clean', 'tank cleaning', 'UC', 900.00, '2026-04-04', '2026-04-09 10:44:32', '2026-04-09 10:44:32'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @misc_category_id
    AND kind = 'expense' AND title = 'Tank Clean' AND amount = 900.00 AND transaction_date = '2026-04-04'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @cash_account_id, @utilities_category_id, NULL, 'expense', 'Taxes', 'house tax + user charge', 'Nagar Nigam', 1300.00, '2026-04-05', '2026-04-09 10:25:38', '2026-04-09 10:25:38'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @cash_account_id AND category_id = @utilities_category_id
    AND kind = 'expense' AND title = 'Taxes' AND amount = 1300.00 AND transaction_date = '2026-04-05'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @credit_card_category_id, NULL, 'expense', 'CC Payment', 'Indusind cc', 'Indusind cc', 3900.00, '2026-04-05', '2026-04-09 10:45:24', '2026-04-09 10:45:24'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @credit_card_category_id
    AND kind = 'expense' AND title = 'CC Payment' AND amount = 3900.00 AND transaction_date = '2026-04-05'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @emi_category_id, NULL, 'expense', 'EMI', 'axis emi', 'axis', 31000.00, '2026-04-05', '2026-04-09 10:45:50', '2026-04-09 10:45:57'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @emi_category_id
    AND kind = 'expense' AND title = 'EMI' AND amount = 31000.00 AND transaction_date = '2026-04-05'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @misc_category_id, NULL, 'expense', 'LinkedIn', 'LinkedIn Subscription', 'LinkedIn', 500.00, '2026-04-11', '2026-04-11 23:15:33', '2026-04-11 23:15:33'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @misc_category_id
    AND kind = 'expense' AND title = 'LinkedIn' AND amount = 500.00 AND transaction_date = '2026-04-11'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @cash_account_id, @food_category_id, NULL, 'expense', 'Food', 'momos', 'stall', 105.00, '2026-04-11', '2026-04-11 23:16:10', '2026-04-11 23:16:10'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @cash_account_id AND category_id = @food_category_id
    AND kind = 'expense' AND title = 'Food' AND amount = 105.00 AND transaction_date = '2026-04-11'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @food_category_id, NULL, 'expense', 'Food', 'Toll + shikanji', NULL, 140.00, '2026-04-13', '2026-04-13 05:42:17', '2026-04-13 05:42:17'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @food_category_id
    AND kind = 'expense' AND title = 'Food' AND amount = 140.00 AND transaction_date = '2026-04-13'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @credit_card_category_id, NULL, 'expense', 'CC', 'amazon CC', 'amazon', 4700.00, '2026-04-13', '2026-04-13 05:43:59', '2026-04-13 05:43:59'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @credit_card_category_id
    AND kind = 'expense' AND title = 'CC' AND amount = 4700.00 AND transaction_date = '2026-04-13'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @credit_card_category_id, NULL, 'expense', 'CC', 'axis CC', 'axis', 233.00, '2026-04-16', '2026-04-16 14:16:04', '2026-04-16 14:16:04'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @credit_card_category_id
    AND kind = 'expense' AND title = 'CC' AND amount = 233.00 AND transaction_date = '2026-04-16'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @utilities_category_id, NULL, 'expense', 'Net + DTH', 'airtel black', 'airtel', 932.00, '2026-04-18', '2026-04-19 08:54:33', '2026-04-19 08:54:33'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @utilities_category_id
    AND kind = 'expense' AND title = 'Net + DTH' AND amount = 932.00 AND transaction_date = '2026-04-18'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @credit_card_category_id, NULL, 'expense', 'CC', 'axis myzone', 'axis', 1200.00, '2026-04-19', '2026-04-19 08:54:57', '2026-04-19 08:54:57'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @credit_card_category_id
    AND kind = 'expense' AND title = 'CC' AND amount = 1200.00 AND transaction_date = '2026-04-19'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @investment_category_id, NULL, 'expense', 'ULIP', 'ulip', 'Tata AIA', 2499.00, '2026-04-24', '2026-04-25 11:22:11', '2026-04-25 11:22:11'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @investment_category_id
    AND kind = 'expense' AND title = 'ULIP' AND amount = 2499.00 AND transaction_date = '2026-04-24'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @upi_account_id, @misc_category_id, NULL, 'expense', 'Haircut', NULL, 'barber', 150.00, '2026-04-26', '2026-04-28 14:39:35', '2026-04-28 14:39:35'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @upi_account_id AND category_id = @misc_category_id
    AND kind = 'expense' AND title = 'Haircut' AND amount = 150.00 AND transaction_date = '2026-04-26'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @misc_category_id, NULL, 'expense', 'Kite Fund', NULL, 'Zerodha', 100.00, '2026-04-28', '2026-04-28 14:40:03', '2026-04-28 14:40:03'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @misc_category_id
    AND kind = 'expense' AND title = 'Kite Fund' AND amount = 100.00 AND transaction_date = '2026-04-28'
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at, updated_at)
SELECT @demo_user_id, @bank_account_id, @groceries_category_id, NULL, 'expense', 'Home things', NULL, 'Big basket', 1513.00, '2026-04-28', '2026-04-28 14:40:38', '2026-04-28 14:40:38'
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE user_id = @demo_user_id AND account_id = @bank_account_id AND category_id = @groceries_category_id
    AND kind = 'expense' AND title = 'Home things' AND amount = 1513.00 AND transaction_date = '2026-04-28'
  LIMIT 1
);

INSERT INTO recurring_transactions (
  user_id, account_id, category_id, kind, title, notes, merchant, amount,
  frequency, day_of_month, start_date, next_due_date, auto_create, is_active,
  created_at, updated_at
)
SELECT @demo_user_id, @bank_account_id, @emi_category_id, 'expense', 'EMI',
  'personal loan monthly EMI', 'Axis', 31000.00, 'monthly', 5,
  '2026-05-05', '2026-05-05', TRUE, TRUE,
  '2026-04-25 11:23:29', '2026-04-25 11:23:29'
WHERE NOT EXISTS (
  SELECT 1 FROM recurring_transactions
  WHERE user_id = @demo_user_id AND title = 'EMI' AND amount = 31000.00 AND day_of_month = 5
  LIMIT 1
);

INSERT INTO recurring_transactions (
  user_id, account_id, category_id, kind, title, notes, merchant, amount,
  frequency, day_of_month, start_date, next_due_date, auto_create, is_active,
  created_at, updated_at
)
SELECT @demo_user_id, @bank_account_id, @investment_category_id, 'expense', 'ULIP',
  'monthly ULIP', 'Tata AIA', 2499.00, 'monthly', 24,
  '2026-05-24', '2026-05-24', TRUE, TRUE,
  '2026-04-25 11:24:22', '2026-04-25 11:24:22'
WHERE NOT EXISTS (
  SELECT 1 FROM recurring_transactions
  WHERE user_id = @demo_user_id AND title = 'ULIP' AND amount = 2499.00 AND day_of_month = 24
  LIMIT 1
);

INSERT INTO recurring_transactions (
  user_id, account_id, category_id, kind, title, notes, merchant, amount,
  frequency, day_of_month, start_date, next_due_date, auto_create, is_active,
  created_at, updated_at
)
SELECT @demo_user_id, @bank_account_id, @utilities_category_id, 'expense', 'Airtel Net',
  'Internet + DTH', 'Airtel', 932.00, 'monthly', 19,
  '2026-05-19', '2026-05-19', TRUE, TRUE,
  '2026-04-25 11:25:11', '2026-04-25 11:25:11'
WHERE NOT EXISTS (
  SELECT 1 FROM recurring_transactions
  WHERE user_id = @demo_user_id AND title = 'Airtel Net' AND amount = 932.00 AND day_of_month = 19
  LIMIT 1
);

INSERT INTO activity_logs (user_id, entity_type, entity_id, action, title, note, created_at)
SELECT @demo_user_id, 'transaction', (
    SELECT id FROM transactions
    WHERE user_id = @demo_user_id AND title = 'ULIP' AND amount = 2499.00 AND transaction_date = '2026-04-24'
    ORDER BY id DESC LIMIT 1
  ), 'create', 'Created transaction ULIP', NULL, '2026-04-25 11:22:11'
WHERE NOT EXISTS (
  SELECT 1 FROM activity_logs
  WHERE user_id = @demo_user_id AND action = 'create' AND title = 'Created transaction ULIP' AND created_at = '2026-04-25 11:22:11'
  LIMIT 1
);

INSERT INTO activity_logs (user_id, entity_type, entity_id, action, title, note, created_at)
SELECT @demo_user_id, 'recurring_transaction', (
    SELECT id FROM recurring_transactions
    WHERE user_id = @demo_user_id AND title = 'EMI' AND amount = 31000.00 AND day_of_month = 5
    ORDER BY id DESC LIMIT 1
  ), 'create', 'Created recurring transaction EMI', NULL, '2026-04-25 11:23:29'
WHERE NOT EXISTS (
  SELECT 1 FROM activity_logs
  WHERE user_id = @demo_user_id AND action = 'create' AND title = 'Created recurring transaction EMI' AND created_at = '2026-04-25 11:23:29'
  LIMIT 1
);

INSERT INTO activity_logs (user_id, entity_type, entity_id, action, title, note, created_at)
SELECT @demo_user_id, 'recurring_transaction', (
    SELECT id FROM recurring_transactions
    WHERE user_id = @demo_user_id AND title = 'ULIP' AND amount = 2499.00 AND day_of_month = 24
    ORDER BY id DESC LIMIT 1
  ), 'create', 'Created recurring transaction ULIP', NULL, '2026-04-25 11:24:22'
WHERE NOT EXISTS (
  SELECT 1 FROM activity_logs
  WHERE user_id = @demo_user_id AND action = 'create' AND title = 'Created recurring transaction ULIP' AND created_at = '2026-04-25 11:24:22'
  LIMIT 1
);

INSERT INTO activity_logs (user_id, entity_type, entity_id, action, title, note, created_at)
SELECT @demo_user_id, 'recurring_transaction', (
    SELECT id FROM recurring_transactions
    WHERE user_id = @demo_user_id AND title = 'Airtel Net' AND amount = 932.00 AND day_of_month = 19
    ORDER BY id DESC LIMIT 1
  ), 'create', 'Created recurring transaction Airtel Net', NULL, '2026-04-25 11:25:11'
WHERE NOT EXISTS (
  SELECT 1 FROM activity_logs
  WHERE user_id = @demo_user_id AND action = 'create' AND title = 'Created recurring transaction Airtel Net' AND created_at = '2026-04-25 11:25:11'
  LIMIT 1
);

INSERT INTO activity_logs (user_id, entity_type, entity_id, action, title, note, created_at)
SELECT @demo_user_id, 'transaction', (
    SELECT id FROM transactions
    WHERE user_id = @demo_user_id AND title = 'Haircut' AND amount = 150.00 AND transaction_date = '2026-04-26'
    ORDER BY id DESC LIMIT 1
  ), 'create', 'Created transaction Haircut', NULL, '2026-04-28 14:39:35'
WHERE NOT EXISTS (
  SELECT 1 FROM activity_logs
  WHERE user_id = @demo_user_id AND action = 'create' AND title = 'Created transaction Haircut' AND created_at = '2026-04-28 14:39:35'
  LIMIT 1
);

INSERT INTO activity_logs (user_id, entity_type, entity_id, action, title, note, created_at)
SELECT @demo_user_id, 'transaction', (
    SELECT id FROM transactions
    WHERE user_id = @demo_user_id AND title = 'Kite Fund' AND amount = 100.00 AND transaction_date = '2026-04-28'
    ORDER BY id DESC LIMIT 1
  ), 'create', 'Created transaction Kite Fund', NULL, '2026-04-28 14:40:03'
WHERE NOT EXISTS (
  SELECT 1 FROM activity_logs
  WHERE user_id = @demo_user_id AND action = 'create' AND title = 'Created transaction Kite Fund' AND created_at = '2026-04-28 14:40:03'
  LIMIT 1
);

INSERT INTO activity_logs (user_id, entity_type, entity_id, action, title, note, created_at)
SELECT @demo_user_id, 'transaction', (
    SELECT id FROM transactions
    WHERE user_id = @demo_user_id AND title = 'Home things' AND amount = 1513.00 AND transaction_date = '2026-04-28'
    ORDER BY id DESC LIMIT 1
  ), 'create', 'Created transaction Home things', NULL, '2026-04-28 14:40:38'
WHERE NOT EXISTS (
  SELECT 1 FROM activity_logs
  WHERE user_id = @demo_user_id AND action = 'create' AND title = 'Created transaction Home things' AND created_at = '2026-04-28 14:40:38'
  LIMIT 1
);
