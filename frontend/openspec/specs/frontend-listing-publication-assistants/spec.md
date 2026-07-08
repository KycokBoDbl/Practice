## Purpose

Defines frontend helper behavior that makes landlord listing publication faster and safer without changing the backend contract.

## Requirements

### Requirement: Publication form supports Russian city autocomplete
The publication form SHALL provide a Russia-only city autocomplete that assists entry without preventing custom city values.

#### Scenario: User types a city prefix
- **WHEN** the user types into the city field
- **THEN** the frontend SHALL show matching Russian city suggestions from a broad built-in list and cities already present in loaded catalog data

#### Scenario: User selects a city suggestion
- **WHEN** the user selects a suggested city
- **THEN** the city field SHALL be filled with that city value

#### Scenario: User enters missing city
- **WHEN** the needed Russian city is not present in suggestions
- **THEN** the user SHALL still be able to submit the typed city value if it passes validation

### Requirement: Publication form provides compact amenity chips
The publication form SHALL provide a compact selectable chip block for common venue amenities and use selected chips in the submitted listing description.

#### Scenario: Amenity chip is selected
- **WHEN** the landlord selects an amenity chip
- **THEN** the selected chip SHALL be visibly marked as selected without submitting the form

#### Scenario: Amenity chip is deselected
- **WHEN** the landlord selects an already selected amenity chip
- **THEN** the chip SHALL be removed from the selected set

#### Scenario: Listing is submitted with selected chips
- **WHEN** the landlord submits the listing with selected amenity chips
- **THEN** the frontend SHALL include the selected amenities in the submitted description in a readable text format

### Requirement: Publication UI supports image placeholder and summary
The publication UI SHALL help the user review listing data before submission even when no image URL is provided.

#### Scenario: Image URL is empty
- **WHEN** the image URL field is empty
- **THEN** the frontend SHALL show a stable placeholder image state

#### Scenario: Price is entered
- **WHEN** the landlord enters price per hour
- **THEN** the frontend SHALL display a localized ruble-per-hour formatted value near the form or summary

#### Scenario: Summary is visible before submit
- **WHEN** the landlord has entered publication details
- **THEN** the frontend SHALL show a compact summary containing type, city, capacity, price, and selected amenities before publication
