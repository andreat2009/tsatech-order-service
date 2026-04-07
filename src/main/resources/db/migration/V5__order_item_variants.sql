ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS variant_key VARCHAR(128);

ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS variant_display_name VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_order_item_variant_scope ON order_item(order_id, product_id, variant_key);
