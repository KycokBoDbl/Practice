ALTER TABLE listings
    ALTER COLUMN address SET NOT NULL,
    ADD CONSTRAINT chk_listings_title_not_blank CHECK (btrim(title) <> ''),
    ADD CONSTRAINT chk_listings_city_not_blank CHECK (btrim(city) <> ''),
    ADD CONSTRAINT chk_listings_address_not_blank CHECK (btrim(address) <> ''),
    ADD CONSTRAINT chk_listings_price_positive CHECK (price_per_hour > 0),
    ADD CONSTRAINT chk_listings_capacity_positive CHECK (capacity > 0);
