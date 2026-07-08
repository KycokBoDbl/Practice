## ADDED Requirements

### Requirement: Discovery of participant bookings
The booking workflow SHALL allow each booking participant organization to discover its own bookings through the booking inbox capability without requiring clients to know booking ids in advance.

#### Scenario: Participant discovers existing booking
- **WHEN** a booking exists for a tenant organization and a landlord organization
- **THEN** the tenant organization can discover it in its outgoing booking inbox
- **THEN** the landlord organization can discover it in its incoming booking inbox

#### Scenario: Non-participant cannot discover booking
- **WHEN** an authenticated organization is neither the booking tenant nor the owner organization of the booking listing
- **THEN** the booking is absent from that organization's inbox results

#### Scenario: Lifecycle behavior is unchanged by discovery
- **WHEN** a participant reads booking inbox results
- **THEN** existing create, detail, history, approve, reject, confirm, cancel, scheduler, conflict, and authorization behavior remains unchanged
