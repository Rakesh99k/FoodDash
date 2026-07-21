ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_person_id BIGINT;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_delivery_person;
ALTER TABLE orders ADD CONSTRAINT fk_orders_delivery_person
    FOREIGN KEY (delivery_person_id) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (
    status IN ('pending', 'confirmed', 'preparing', 'ready_for_pickup', 'out_for_delivery', 'delivered', 'cancelled')
);

ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider_order_id VARCHAR(120);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider_reference VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider_client_secret VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS webhook_event_id VARCHAR(120);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS failure_reason TEXT;

UPDATE payments SET provider = 'stripe' WHERE provider IS NULL;
ALTER TABLE payments ALTER COLUMN provider SET NOT NULL;

ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payments_provider;
ALTER TABLE payments ADD CONSTRAINT chk_payments_provider CHECK (provider IN ('stripe', 'razorpay'));

ALTER TABLE payments DROP CONSTRAINT IF EXISTS uk_payments_webhook_event_id;
ALTER TABLE payments ADD CONSTRAINT uk_payments_webhook_event_id UNIQUE (webhook_event_id);

CREATE INDEX IF NOT EXISTS idx_orders_delivery_person_id ON orders (delivery_person_id);
CREATE INDEX IF NOT EXISTS idx_payments_provider_order_id ON payments (provider_order_id);
CREATE INDEX IF NOT EXISTS idx_payments_provider_payment_id ON payments (provider_payment_id);
CREATE INDEX IF NOT EXISTS idx_payments_webhook_event_id ON payments (webhook_event_id);
