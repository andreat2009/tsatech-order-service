CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total NUMERIC(15,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku VARCHAR(64),
    name VARCHAR(255),
    quantity INT NOT NULL,
    unit_price NUMERIC(15,4) NOT NULL,
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_order_item_order ON order_item(order_id);
CREATE INDEX idx_order_customer ON orders(customer_id);
