ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS discount_total NUMERIC(15,4),
    ADD COLUMN IF NOT EXISTS customer_group_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS applied_coupon_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS applied_offer_codes VARCHAR(512);
