UPDATE accounts
SET name = CASE
  WHEN type = 'bank' THEN 'Bank'
  WHEN type = 'cash' THEN 'Cash'
  WHEN type = 'credit' THEN 'Credit Card'
  WHEN type = 'wallet' THEN 'Wallet'
  ELSE name
END
WHERE user_id = 1;
