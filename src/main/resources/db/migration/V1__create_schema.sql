CREATE TABLE products (
    sku VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT product_price_nonnegative CHECK (price >= 0)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT order_total_nonnegative CHECK (total_amount >= 0),
    CONSTRAINT order_status_valid CHECK (status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_orders_status_created_at ON orders (status, created_at);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    sku VARCHAR(64) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL,
    CONSTRAINT order_item_quantity_positive CHECK (quantity > 0),
    CONSTRAINT order_item_price_nonnegative CHECK (unit_price >= 0),
    CONSTRAINT order_item_total_nonnegative CHECK (line_total >= 0),
    CONSTRAINT order_item_unique_sku UNIQUE (order_id, sku)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
