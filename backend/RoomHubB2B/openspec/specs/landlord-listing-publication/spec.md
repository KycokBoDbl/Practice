# Landlord Listing Publication Specification

## Purpose

Defines authenticated landlord publication of commercial space listings, including ownership, authorization, validation, persistence invariants, and public catalog visibility.

## Requirements

### Requirement: Арендодатель публикует объявление
Система SHALL предоставлять `POST /api/listings`, который создает объявление со статусом `PUBLISHED` для организации аутентифицированного пользователя с ролью `LANDLORD` и отвечает `201 Created` с `ListingResponseDto`.

#### Scenario: Успешная публикация
- **WHEN** аутентифицированный `LANDLORD` отправляет валидные `title`, `description`, `city`, `address`, `pricePerHour`, `capacity`, `spaceType` и `imageUrl`
- **THEN** система отвечает `201 Created` и возвращает созданное объявление с серверным идентификатором и переданными публичными полями
- **AND** сохраненная запись имеет статус `PUBLISHED`, серверное время создания и `owner_organization_id` из access token
- **AND** ответ содержит `Location: /api/listings/{listingId}`

#### Scenario: Новое объявление видно в каталоге
- **WHEN** объявление успешно опубликовано через `POST /api/listings`
- **THEN** последующий публичный `GET /api/listings` включает это объявление в существующем формате `ListingResponseDto`

### Requirement: Владелец определяется из аутентифицированной организации
Система MUST определять владельца нового объявления по `organizationId` из валидного bearer access token и MUST NOT принимать `ownerOrganizationId`, `status` или `createdAt` как управляемые клиентом поля запроса.

#### Scenario: Объявление привязано к организации токена
- **WHEN** `LANDLORD` публикует объявление с токеном организации `17`
- **THEN** система сохраняет `owner_organization_id = 17`
- **AND** клиент не может назначить объявление другой организации через request body

### Requirement: Публикация ограничена ролью арендодателя
Система SHALL разрешать публикацию объявлений только аутентифицированным пользователям с ролью `LANDLORD`.

#### Scenario: Токен отсутствует или невалиден
- **WHEN** клиент отправляет `POST /api/listings` без валидного bearer access token
- **THEN** система отвечает `401 Unauthorized` и не создает объявление

#### Scenario: Арендатор пытается опубликовать объявление
- **WHEN** аутентифицированный пользователь с ролью `TENANT` отправляет валидный `POST /api/listings`
- **THEN** система отвечает `403 Forbidden` и не создает объявление

### Requirement: Данные публикации валидируются до сохранения
Система SHALL отклонять публикацию с `400 Bad Request`, если `title`, `city`, `address` или `spaceType` отсутствуют, обязательный текст пуст, `title` длиннее 255 символов, `city` длиннее 100 символов, `address` длиннее 255 символов, `pricePerHour` не является положительной суммой в пределах `NUMERIC(10,2)`, `capacity` не является положительным целым числом либо непустой `imageUrl` не является HTTP(S) URL.

#### Scenario: Обязательное поле невалидно
- **WHEN** `LANDLORD` отправляет публикацию с пустым `title`, неположительной `pricePerHour` или неположительной `capacity` при поддерживаемом `spaceType`
- **THEN** система отвечает `400 Bad Request` с `ProblemDetail`, содержащим ошибки полей
- **AND** объявление не сохраняется

#### Scenario: Тип помещения не поддерживается
- **WHEN** `LANDLORD` отправляет публикацию с неизвестным значением `spaceType`
- **THEN** система отвечает `400 Bad Request` с `ProblemDetail.errors`, содержащим field error для `spaceType`
- **AND** объявление не сохраняется

#### Scenario: Необязательные поля отсутствуют
- **WHEN** `LANDLORD` отправляет валидную публикацию без `description` и `imageUrl`
- **THEN** система создает объявление и отвечает `201 Created`
- **AND** `description` и `imageUrl` имеют значение `null` в JSON и nullable-типы в OpenAPI response schema

### Requirement: Инварианты объявления защищены базой данных
PostgreSQL SHALL отклонять listing с пустыми `title`, `city` или `address`, отсутствующим `address`, неположительной `price_per_hour` или неположительной `capacity` независимо от способа записи.

#### Scenario: Запись обходит HTTP validation
- **WHEN** клиент БД пытается вставить listing, нарушающий текстовый, ценовой или capacity constraint
- **THEN** PostgreSQL отклоняет запись с constraint violation

### Requirement: Listing publication stores geocoded coordinates
The system SHALL geocode the submitted `city` and `address` with the configured Yandex Maps Geocoder before creating a listing, store the resolved `latitude` and `longitude` on the listing record, and include both fields in the `ListingResponseDto` returned by `POST /api/listings`.

#### Scenario: Published listing receives coordinates
- **WHEN** an authenticated `LANDLORD` sends `POST /api/listings` with valid listing fields and an address that Yandex Geocoder resolves
- **THEN** the system creates the listing with status `PUBLISHED`
- **AND** the `listings` row stores both `latitude` and `longitude`
- **AND** the response body includes numeric `latitude` and `longitude` fields for the created listing

#### Scenario: Unresolvable address is rejected
- **WHEN** an authenticated `LANDLORD` sends `POST /api/listings` with valid listing fields but Yandex Geocoder returns no usable coordinate result for the submitted `city` and `address`
- **THEN** the system returns `400 Bad Request` with `ProblemDetail`
- **AND** no listing is created

#### Scenario: Geocoder integration failure prevents incomplete listing
- **WHEN** an authenticated `LANDLORD` sends `POST /api/listings` with valid listing fields but the geocoder request times out, returns a non-success response, or returns an invalid payload
- **THEN** the system returns a `ProblemDetail` error
- **AND** no listing is created without coordinates

### Requirement: Listing coordinates are server-owned
The system MUST derive listing coordinates from the stored `city` and `address` and MUST NOT accept `latitude` or `longitude` as client-controlled fields in `CreateListingRequestDto`.

#### Scenario: Client cannot provide publication coordinates
- **WHEN** a client sends `POST /api/listings` with `latitude` or `longitude` fields in the JSON request body
- **THEN** the system ignores those fields or rejects them according to existing request-body handling
- **AND** any stored coordinates come from backend geocoding, not from the client payload
