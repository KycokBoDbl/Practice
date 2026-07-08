## ADDED Requirements

### Requirement: Booking inbox contract is published
The OpenAPI document SHALL include the booking inbox endpoint, its supported query parameters, successful response schema, and documented error responses.

#### Scenario: Inbox endpoint is present in OpenAPI
- **WHEN** a client reads `GET /api/openapi`
- **THEN** the OpenAPI document contains the booking inbox path
- **THEN** the operation documents bearer authentication, `200`, `400`, and `401` responses

#### Scenario: Inbox response schema is documented
- **WHEN** a frontend developer inspects the successful inbox response schema
- **THEN** each item schema documents id, listingId, listingTitle, status, startAt, endAt, pricePerHour, totalPrice, confirmationDeadline, tenantOrganizationName, landlordOrganizationName, createdAt, and updatedAt using camelCase names

#### Scenario: Exported contract contains inbox changes
- **WHEN** the OpenAPI export command is run after implementing the booking inbox endpoint
- **THEN** `openapi/roomhub-b2b.openapi.json` contains the same booking inbox path and schemas as the runtime OpenAPI document
