ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS address VARCHAR(255);

UPDATE restaurants
SET address = COALESCE(NULLIF(address_line1, ''), 'Unknown address')
WHERE address IS NULL;

ALTER TABLE restaurants ALTER COLUMN address SET NOT NULL;

ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS cuisine_type VARCHAR(100) NOT NULL DEFAULT 'General';

ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE restaurants
SET status = CASE
    WHEN is_active = TRUE THEN 'ACTIVE'
    ELSE 'CLOSED'
END
WHERE status IS NULL OR status NOT IN ('ACTIVE', 'CLOSED');

ALTER TABLE restaurants DROP CONSTRAINT IF EXISTS chk_restaurants_status;
ALTER TABLE restaurants ADD CONSTRAINT chk_restaurants_status CHECK (status IN ('ACTIVE', 'CLOSED'));

ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_restaurants_status ON restaurants (status);
CREATE INDEX IF NOT EXISTS idx_restaurants_cuisine_type ON restaurants (cuisine_type);
CREATE INDEX IF NOT EXISTS idx_restaurants_is_deleted ON restaurants (is_deleted);
CREATE INDEX IF NOT EXISTS idx_restaurants_location_lat_lng ON restaurants (location_lat, location_lng);
