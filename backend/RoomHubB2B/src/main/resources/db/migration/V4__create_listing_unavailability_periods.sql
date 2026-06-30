CREATE TABLE listing_unavailability_periods (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    start_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_listing_unavailability_period_order CHECK (end_at > start_at),
    CONSTRAINT chk_listing_unavailability_start_whole_hour
        CHECK (start_at = date_trunc('hour', start_at)),
    CONSTRAINT chk_listing_unavailability_end_whole_hour
        CHECK (end_at = date_trunc('hour', end_at))
);

CREATE INDEX idx_listing_unavailability_periods_overlap
    ON listing_unavailability_periods (listing_id, start_at, end_at);
