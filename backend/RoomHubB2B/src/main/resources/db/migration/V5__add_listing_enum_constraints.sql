ALTER TABLE listings
    ADD CONSTRAINT chk_listings_space_type
        CHECK (space_type IN (
            'MEETING_ROOM',
            'CONFERENCE_HALL',
            'CLASSROOM',
            'LOFT',
            'SHOWROOM'
        )),
    ADD CONSTRAINT chk_listings_status
        CHECK (status IN ('PUBLISHED'));
