INSERT INTO
    inventory (product_id, product_name, price, stock, reserved)
VALUES
    (1, 'iPhone 16', 7999, 1000, 0),
    (2, 'MacBook Pro', 19999, 500, 0),
    (3, 'iPad Air', 5999, 750, 0),
    (4, 'Apple Watch', 3999, 300, 0) ON DUPLICATE KEY
UPDATE product_name =
VALUES
    (product_name),
    price =
VALUES
    (price),
    stock =
VALUES
    (stock);

-- Verify data
SELECT
    *
FROM
    inventory;