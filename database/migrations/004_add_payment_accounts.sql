INSERT INTO accounts (id, user_id, name, type, balance)
VALUES
  (4, 1, 'UPI', 'bank', 0.00),
  (5, 1, 'UPI-Lite', 'bank', 0.00),
  (6, 1, 'NEFT', 'bank', 0.00)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  type = VALUES(type),
  balance = VALUES(balance);
