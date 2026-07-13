-- Preflight duplicate check for existing environments:
-- SELECT tenant_organization_id, listing_id, start_at::date AS booking_date, count(*) AS duplicate_count
-- FROM bookings
-- GROUP BY tenant_organization_id, listing_id, start_at::date
-- HAVING count(*) > 1;

WITH ranked_duplicate_bookings AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY tenant_organization_id, listing_id, start_at::date
            ORDER BY
                CASE status
                    WHEN 'IN_PROGRESS' THEN 1
                    WHEN 'CONFIRMED' THEN 2
                    WHEN 'AWAITING_CONFIRMATION' THEN 3
                    WHEN 'REQUESTED' THEN 4
                    WHEN 'COMPLETED' THEN 5
                    WHEN 'CANCELLED' THEN 6
                    WHEN 'REJECTED' THEN 7
                    WHEN 'EXPIRED' THEN 8
                    ELSE 9
                END,
                created_at,
                id
        ) AS duplicate_rank
    FROM bookings
)
DELETE FROM bookings
USING ranked_duplicate_bookings
WHERE bookings.id = ranked_duplicate_bookings.id
  AND ranked_duplicate_bookings.duplicate_rank > 1;

CREATE UNIQUE INDEX uk_bookings_tenant_listing_start_date
    ON bookings (tenant_organization_id, listing_id, (start_at::date));
