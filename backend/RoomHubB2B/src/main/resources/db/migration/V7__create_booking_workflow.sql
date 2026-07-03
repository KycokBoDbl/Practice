ALTER TABLE listings
    ADD COLUMN owner_organization_id BIGINT,
    ADD CONSTRAINT fk_listings_owner_organization
        FOREIGN KEY (owner_organization_id) REFERENCES organizations (id);

CREATE INDEX idx_listings_owner_organization_id
    ON listings (owner_organization_id);

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    tenant_organization_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    start_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    price_per_hour NUMERIC(10, 2) NOT NULL,
    total_price NUMERIC(14, 2) NOT NULL,
    confirmation_deadline TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_listing
        FOREIGN KEY (listing_id) REFERENCES listings (id),
    CONSTRAINT fk_bookings_tenant_organization
        FOREIGN KEY (tenant_organization_id) REFERENCES organizations (id),
    CONSTRAINT chk_bookings_status CHECK (status IN (
        'REQUESTED',
        'AWAITING_CONFIRMATION',
        'CONFIRMED',
        'IN_PROGRESS',
        'COMPLETED',
        'REJECTED',
        'CANCELLED',
        'EXPIRED'
    )),
    CONSTRAINT chk_bookings_period_order CHECK (end_at > start_at),
    CONSTRAINT chk_bookings_start_whole_hour CHECK (start_at = date_trunc('hour', start_at)),
    CONSTRAINT chk_bookings_end_whole_hour CHECK (end_at = date_trunc('hour', end_at)),
    CONSTRAINT chk_bookings_price_per_hour CHECK (price_per_hour >= 0),
    CONSTRAINT chk_bookings_total_price CHECK (total_price >= 0),
    CONSTRAINT chk_bookings_confirmation_deadline CHECK (
        confirmation_deadline IS NULL OR confirmation_deadline <= start_at
    )
);

CREATE INDEX idx_bookings_listing_id ON bookings (listing_id);
CREATE INDEX idx_bookings_tenant_organization_id ON bookings (tenant_organization_id);
CREATE INDEX idx_bookings_due_confirmation
    ON bookings (confirmation_deadline, id) WHERE status = 'AWAITING_CONFIRMATION';
CREATE INDEX idx_bookings_due_start
    ON bookings (start_at, id) WHERE status = 'CONFIRMED';
CREATE INDEX idx_bookings_due_end
    ON bookings (end_at, id) WHERE status = 'IN_PROGRESS';

CREATE TABLE booking_status_history (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_status_history_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT chk_booking_status_history_from CHECK (
        from_status IS NULL OR from_status IN (
            'REQUESTED', 'AWAITING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS',
            'COMPLETED', 'REJECTED', 'CANCELLED', 'EXPIRED'
        )
    ),
    CONSTRAINT chk_booking_status_history_to CHECK (to_status IN (
        'REQUESTED', 'AWAITING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS',
        'COMPLETED', 'REJECTED', 'CANCELLED', 'EXPIRED'
    )),
    CONSTRAINT chk_booking_status_history_reason CHECK (btrim(reason) <> '')
);

CREATE INDEX idx_booking_status_history_booking_order
    ON booking_status_history (booking_id, created_at, id);

ALTER TABLE listing_unavailability_periods
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN booking_id BIGINT,
    ADD CONSTRAINT fk_listing_unavailability_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    ADD CONSTRAINT uk_listing_unavailability_booking UNIQUE (booking_id),
    ADD CONSTRAINT chk_listing_unavailability_source CHECK (source IN ('MANUAL', 'BOOKING')),
    ADD CONSTRAINT chk_listing_unavailability_source_booking CHECK (
        (source = 'MANUAL' AND booking_id IS NULL)
        OR (source = 'BOOKING' AND booking_id IS NOT NULL)
    );
