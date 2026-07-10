ALTER TABLE listings
    DROP CONSTRAINT chk_listings_status,
    ADD CONSTRAINT chk_listings_status
        CHECK (status IN ('PUBLISHED', 'ARCHIVED'));
