ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_first_name VARCHAR(128);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_last_name VARCHAR(128);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(64);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_locale VARCHAR(8);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_comment VARCHAR(2000);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS guest_checkout BOOLEAN;

UPDATE orders SET guest_checkout = FALSE WHERE guest_checkout IS NULL;
ALTER TABLE orders ALTER COLUMN guest_checkout SET DEFAULT FALSE;
ALTER TABLE orders ALTER COLUMN guest_checkout SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_customer_email ON orders(customer_email);
CREATE INDEX IF NOT EXISTS idx_orders_guest_checkout ON orders(guest_checkout);
