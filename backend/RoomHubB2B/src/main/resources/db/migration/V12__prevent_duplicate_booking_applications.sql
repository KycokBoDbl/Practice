-- Preflight duplicate check for existing environments:
-- SELECT tenant_organization_id, listing_id, start_at::date AS booking_date, count(*) AS duplicate_count
-- FROM bookings
-- GROUP BY tenant_organization_id, listing_id, start_at::date
-- HAVING count(*) > 1;

CREATE UNIQUE INDEX uk_bookings_tenant_listing_start_date
    ON bookings (tenant_organization_id, listing_id, (start_at::date));
