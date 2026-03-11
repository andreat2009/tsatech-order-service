CREATE TABLE order_return (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT,
    customer_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    comment TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_order_return_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_return_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_item(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_order_return_order ON order_return(order_id);
CREATE INDEX idx_order_return_customer ON order_return(customer_id);
