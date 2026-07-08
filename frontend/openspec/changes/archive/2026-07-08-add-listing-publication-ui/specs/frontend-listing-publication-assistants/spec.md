## ADDED Requirements

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
The publication form SHALL provide a compact selectable chip block for common venue amenities and use selected chips in the listing description.

#### Scenario: Amenity chip is selected
- **WHEN** the landlord selects an amenity chip
- **THEN** the selected chip SHALL be visibly marked as selected without submitting the form

#### Scenario: Amenity chip is deselected
- **WHEN** the landlord selects an already selected amenity chip
- **THEN** the chip SHALL be removed from the selected set

#### Scenario: Listing is submitted with selected chips
- **WHEN** the landlord submits the listing with selected amenity chips
- **THEN** the frontend SHALL include the selected amenities in the submitted description in a readable text format

#### Scenario: Many chips are available
- **WHEN** the amenity chip base contains many options
- **THEN** the frontend SHALL present them compactly through grouping, search, or progressive display so the form remains usable

### Requirement: Publication UI supports image placeholder
The publication UI SHALL avoid broken image presentation when no image URL is provided.

#### Scenario: Image URL is empty
- **WHEN** the image URL field is empty
- **THEN** the frontend SHALL show a placeholder image state wherever publication UI needs to represent the listing image

#### Scenario: Image URL is provided
- **WHEN** the image URL field contains a valid URL
- **THEN** the frontend SHALL use that URL where the publication UI represents the listing image

### Requirement: Publication UI formats price and shows final summary
The publication UI SHALL help landlords verify key values before submitting.

#### Scenario: Price is entered
- **WHEN** the landlord enters price per hour
- **THEN** the frontend SHALL display a localized ruble-per-hour formatted value near the form or summary

#### Scenario: Summary is visible before submit
- **WHEN** the landlord has entered publication details
- **THEN** the frontend SHALL show a compact summary containing type, city, capacity, price, and selected amenities before publication
