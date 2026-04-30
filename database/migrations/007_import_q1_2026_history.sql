SET @demo_user_id := 1;

INSERT INTO categories (user_id, name, type, color, icon, is_default, is_archived, budget_mode)
SELECT @demo_user_id, 'Credit Card', 'expense', '#22ec13', 'tag', FALSE, FALSE, 'flexible'
WHERE NOT EXISTS (
  SELECT 1
  FROM categories
  WHERE user_id = @demo_user_id
    AND LOWER(name) = LOWER('Credit Card')
    AND type = 'expense'
  LIMIT 1
);

INSERT INTO categories (user_id, name, type, color, icon, is_default, is_archived, budget_mode)
SELECT @demo_user_id, 'EMI', 'expense', '#ed0c45', 'EMI', FALSE, FALSE, 'flexible'
WHERE NOT EXISTS (
  SELECT 1
  FROM categories
  WHERE user_id = @demo_user_id
    AND LOWER(name) = LOWER('EMI')
    AND type = 'expense'
  LIMIT 1
);

INSERT INTO categories (user_id, name, type, color, icon, is_default, is_archived, budget_mode)
SELECT @demo_user_id, 'Misc', 'expense', '#0f766e', 'tag', FALSE, FALSE, 'flexible'
WHERE NOT EXISTS (
  SELECT 1
  FROM categories
  WHERE user_id = @demo_user_id
    AND LOWER(name) = LOWER('Misc')
    AND type = 'expense'
  LIMIT 1
);

INSERT INTO categories (user_id, name, type, color, icon, is_default, is_archived, budget_mode)
SELECT @demo_user_id, 'Food', 'expense', '#0f766e', 'tag', FALSE, FALSE, 'flexible'
WHERE NOT EXISTS (
  SELECT 1
  FROM categories
  WHERE user_id = @demo_user_id
    AND LOWER(name) = LOWER('Food')
    AND type = 'expense'
  LIMIT 1
);

INSERT INTO categories (user_id, name, type, color, icon, is_default, is_archived, budget_mode)
SELECT @demo_user_id, 'Investment', 'expense', '#0f766e', 'tag', FALSE, FALSE, 'flexible'
WHERE NOT EXISTS (
  SELECT 1
  FROM categories
  WHERE user_id = @demo_user_id
    AND LOWER(name) = LOWER('Investment')
    AND type = 'expense'
  LIMIT 1
);

INSERT INTO categories (user_id, name, type, color, icon, is_default, is_archived, budget_mode)
SELECT @demo_user_id, 'Hotel', 'expense', '#7c2d12', 'bed', FALSE, FALSE, 'flexible'
WHERE NOT EXISTS (
  SELECT 1
  FROM categories
  WHERE user_id = @demo_user_id
    AND LOWER(name) = LOWER('Hotel')
    AND type = 'expense'
  LIMIT 1
);

SET @upi_account_id := (
  SELECT id
  FROM accounts
  WHERE user_id = @demo_user_id
    AND name = 'UPI'
  LIMIT 1
);
SET @salary_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Salary'
    AND type = 'income'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
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
SET @transport_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Transport'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);
SET @healthcare_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Healthcare'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);
SET @entertainment_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Entertainment'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);
SET @shopping_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Shopping'
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
SET @hotel_category_id := (
  SELECT id
  FROM categories
  WHERE (user_id IS NULL OR user_id = @demo_user_id)
    AND name = 'Hotel'
    AND type = 'expense'
  ORDER BY user_id DESC, id DESC
  LIMIT 1
);

INSERT INTO transactions (user_id, account_id, category_id, kind, title, notes, merchant, amount, transaction_date)
VALUES
  (@demo_user_id, @upi_account_id, @salary_category_id, 'income', 'Salary', 'Imported historical data', NULL, 133000.00, '2026-01-01'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 950.00, '2026-01-01'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Petrol', 'Imported historical data', NULL, 250.00, '2026-01-01'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 1130.00, '2026-01-02'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Home', 'Imported historical data', NULL, 3000.00, '2026-01-03'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Misc', 'Imported historical data', NULL, 1700.00, '2026-01-03'),
  (@demo_user_id, @upi_account_id, @emi_category_id, 'expense', 'EMI', 'Imported historical data', NULL, 31000.00, '2026-01-05'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 1981.00, '2026-01-05'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 100.00, '2026-01-10'),
  (@demo_user_id, @upi_account_id, @investment_category_id, 'expense', 'Investment', 'Imported historical data', NULL, 70000.00, '2026-01-12'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Home', 'Imported historical data', NULL, 13000.00, '2026-01-12'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Apay', 'Imported historical data', NULL, 1000.00, '2026-01-12'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Lite', 'Imported historical data', NULL, 1000.00, '2026-01-12'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 8888.00, '2026-01-13'),
  (@demo_user_id, @upi_account_id, @entertainment_category_id, 'expense', 'Netflix', 'Imported historical data', NULL, 199.00, '2026-01-15'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 1281.00, '2026-01-16'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 1100.00, '2026-01-17'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Net', 'Imported historical data', NULL, 932.00, '2026-01-18'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 497.00, '2026-01-19'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Bus', 'Imported historical data', NULL, 800.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Auto', 'Imported historical data', NULL, 300.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Auto', 'Imported historical data', NULL, 200.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @hotel_category_id, 'expense', 'Hotel', 'Imported historical data', NULL, 2000.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 120.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Bike', 'Imported historical data', NULL, 34.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Cab', 'Imported historical data', NULL, 530.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Metro', 'Imported historical data', NULL, 94.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 230.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Snacks', 'Imported historical data', NULL, 306.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Auto', 'Imported historical data', NULL, 79.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @shopping_category_id, 'expense', 'Shopping', 'Imported historical data', NULL, 1000.00, '2026-01-20'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 150.00, '2026-01-21'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Snacks', 'Imported historical data', NULL, 30.00, '2026-01-21'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Metro', 'Imported historical data', NULL, 34.00, '2026-01-21'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Tip', 'Imported historical data', NULL, 50.00, '2026-01-21'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 40.00, '2026-01-21'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 60.00, '2026-01-21'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Cab', 'Imported historical data', NULL, 191.00, '2026-01-21'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Metro', 'Imported historical data', NULL, 50.00, '2026-01-22'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Cab', 'Imported historical data', NULL, 82.00, '2026-01-22'),
  (@demo_user_id, @upi_account_id, @investment_category_id, 'expense', 'ULIP', 'Imported historical data', NULL, 2499.00, '2026-01-24'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Misc', 'Imported historical data', NULL, 1700.00, '2026-01-24'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Dry Clean', 'Imported historical data', NULL, 2000.00, '2026-01-25'),
  (@demo_user_id, @upi_account_id, @healthcare_category_id, 'expense', 'Meds', 'Imported historical data', NULL, 420.00, '2026-01-28'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 60.00, '2026-01-28'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Tailor', 'Imported historical data', NULL, 400.00, '2026-01-30'),

  (@demo_user_id, @upi_account_id, @salary_category_id, 'income', 'Salary', 'Imported historical data', NULL, 133000.00, '2026-02-01'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 460.00, '2026-02-01'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Charger', 'Imported historical data', NULL, 171.00, '2026-02-02'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Dry Clean', 'Imported historical data', NULL, 250.00, '2026-02-03'),
  (@demo_user_id, @upi_account_id, @emi_category_id, 'expense', 'EMI', 'Imported historical data', NULL, 31000.00, '2026-02-05'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Newspaper', 'Imported historical data', NULL, 210.00, '2026-02-07'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Apay', 'Imported historical data', NULL, 2000.00, '2026-02-08'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Ghar', 'Imported historical data', NULL, 23000.00, '2026-02-08'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Misc', 'Imported historical data', NULL, 200.00, '2026-02-08'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Beer', 'Imported historical data', NULL, 400.00, '2026-02-12'),
  (@demo_user_id, @upi_account_id, @healthcare_category_id, 'expense', 'Dentist', 'Imported historical data', NULL, 3500.00, '2026-02-13'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 3200.00, '2026-02-13'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Apay', 'Imported historical data', NULL, 1000.00, '2026-02-14'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Lite', 'Imported historical data', NULL, 1000.00, '2026-02-14'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Car', 'Imported historical data', NULL, 5600.00, '2026-02-14'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Misc', 'Imported historical data', NULL, 384.00, '2026-02-15'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 9000.00, '2026-02-16'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Dry Clean', 'Imported historical data', NULL, 600.00, '2026-02-17'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Net', 'Imported historical data', NULL, 932.00, '2026-02-18'),
  (@demo_user_id, @upi_account_id, @groceries_category_id, 'expense', 'Soap', 'Imported historical data', NULL, 207.00, '2026-02-19'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 270.00, '2026-02-20'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Misc', 'Imported historical data', NULL, 2000.00, '2026-02-21'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Kooda', 'Imported historical data', NULL, 100.00, '2026-02-23'),
  (@demo_user_id, @upi_account_id, @investment_category_id, 'expense', 'ULIP', 'Imported historical data', NULL, 2500.00, '2026-02-24'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Misc', 'Imported historical data', NULL, 1000.00, '2026-02-24'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Dry Clean', 'Imported historical data', NULL, 600.00, '2026-02-25'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Misc', 'Imported historical data', NULL, 630.00, '2026-02-26'),
  (@demo_user_id, @upi_account_id, @investment_category_id, 'expense', 'Investment', 'Imported historical data', NULL, 40000.00, '2026-02-27'),

  (@demo_user_id, @upi_account_id, @salary_category_id, 'income', 'Salary', 'Imported historical data', NULL, 133000.00, '2026-03-01'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Net', 'Imported historical data', NULL, 3533.00, '2026-03-01'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 10600.00, '2026-03-02'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Misc', 'Imported historical data', NULL, 325.00, '2026-03-02'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Misc', 'Imported historical data', NULL, 400.00, '2026-03-03'),
  (@demo_user_id, @upi_account_id, @emi_category_id, 'expense', 'EMI', 'Imported historical data', NULL, 31000.00, '2026-03-05'),
  (@demo_user_id, @upi_account_id, @entertainment_category_id, 'expense', 'Viki', 'Imported historical data', NULL, 299.00, '2026-03-05'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 300.00, '2026-03-06'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Petrol', 'Imported historical data', NULL, 277.00, '2026-03-09'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Hair', 'Imported historical data', NULL, 130.00, '2026-03-09'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 2540.00, '2026-03-14'),
  (@demo_user_id, @upi_account_id, @misc_category_id, 'expense', 'Newspaper', 'Imported historical data', NULL, 196.00, '2026-03-16'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Bier', 'Imported historical data', NULL, 640.00, '2026-03-16'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 30.00, '2026-03-16'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Apay', 'Imported historical data', NULL, 2000.00, '2026-03-16'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 100.00, '2026-03-17'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Net', 'Imported historical data', NULL, 932.00, '2026-03-18'),
  (@demo_user_id, @upi_account_id, @entertainment_category_id, 'expense', 'Movie', 'Imported historical data', NULL, 216.00, '2026-03-19'),
  (@demo_user_id, @upi_account_id, @investment_category_id, 'expense', 'Investment', 'Imported historical data', NULL, 80000.00, '2026-03-19'),
  (@demo_user_id, @upi_account_id, @transport_category_id, 'expense', 'Parking', 'Imported historical data', NULL, 40.00, '2026-03-21'),
  (@demo_user_id, @upi_account_id, @investment_category_id, 'expense', 'ULIP', 'Imported historical data', NULL, 2500.00, '2026-03-24'),
  (@demo_user_id, @upi_account_id, @investment_category_id, 'expense', 'NPS', 'Imported historical data', NULL, 1500.00, '2026-03-24'),
  (@demo_user_id, @upi_account_id, @credit_card_category_id, 'expense', 'CC', 'Imported historical data', NULL, 8000.00, '2026-03-24'),
  (@demo_user_id, @upi_account_id, @utilities_category_id, 'expense', 'Home', 'Imported historical data', NULL, 12000.00, '2026-03-25'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 60.00, '2026-03-25'),
  (@demo_user_id, @upi_account_id, @food_category_id, 'expense', 'Food', 'Imported historical data', NULL, 342.00, '2026-03-27');
