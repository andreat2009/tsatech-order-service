CREATE TABLE order_custom_field_value (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    field_code VARCHAR(128) NOT NULL,
    field_label VARCHAR(255) NOT NULL,
    field_type VARCHAR(32),
    field_scope VARCHAR(32),
    field_value VARCHAR(4000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_order_custom_field_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_order_custom_field UNIQUE (order_id, field_code)
);

CREATE INDEX idx_order_custom_field_order ON order_custom_field_value(order_id);
