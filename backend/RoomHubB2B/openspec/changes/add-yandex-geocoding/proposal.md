## Why

Карточка объявления уже содержит город и адрес, но фронт не получает координаты для отображения помещения на карте. Интеграция с геокодером Яндекс Карт позволит автоматически получать координаты при публикации и изменении объявления, сохранять их в базе и отдавать в существующих listing API.

## What Changes

- При создании объявления через `POST /api/listings` backend вызывает API геокодера Яндекс Карт по адресу объявления и сохраняет координаты в записи `listings`.
- При редактировании объявления через `PUT /api/listings/{listingId}` backend повторно геокодирует адрес, если меняется `city` или `address`, и обновляет сохраненные координаты.
- `ListingResponseDto` и ответы `GET /api/listings`, `POST /api/listings`, `PUT /api/listings/{listingId}` и `GET /api/listings/owned` включают координаты `latitude` и `longitude`, чтобы frontend мог отобразить маркер на карте.
- В базе данных у таблицы `listings` появляются поля для широты и долготы; существующие записи допускают отсутствие координат до успешного обновления или отдельного backfill.
- Интеграция использует ключ геокодера Яндекс Карт из конфигурации через `ROOMHUB_GEOCODING_YANDEX_API_KEY`; реализация не должна хардкодить его в Java-коде или других runtime-артефактах.
- Если геокодер не возвращает координаты или внешний вызов завершается ошибкой, публикация или изменение объявления отклоняется понятной `ProblemDetail` ошибкой, чтобы в базе не появлялось новое или обновленное объявление без координат.

Пример успешного response item:

```json
{
  "id": 42,
  "title": "Meeting room in the city center",
  "city": "Barnaul",
  "address": "Lenina Avenue, 10",
  "pricePerHour": 2500.00,
  "capacity": 20,
  "spaceType": "MEETING_ROOM",
  "imageUrl": "https://example.com/listing-42.jpg",
  "description": "Room with projector",
  "ownerOrganizationName": "Landlord LLC",
  "latitude": 53.348114,
  "longitude": 83.779836
}
```

## Capabilities

### New Capabilities

### Modified Capabilities

- `landlord-listing-publication`: публикация объявления должна геокодировать адрес и сохранять координаты вместе с объявлением.
- `landlord-listing-management`: редактирование объявления должно обновлять координаты при изменении адресных полей и отдавать координаты в owned listing views.
- `openapi-contract-publishing`: OpenAPI contract должен документировать новые поля координат в listing response schemas и обновленный контракт для frontend.

## Impact

- Affected code: `listing` service/entity/DTO/controller flow, validation/error handling, configuration properties for Yandex geocoder, tests for publication and management.
- Database: table `listings` gains nullable numeric coordinate columns such as `latitude` and `longitude`; new listings must populate both values on successful geocoding.
- External system: backend calls Yandex Maps Geocoder API using a configured API key and must handle timeouts, empty results, and non-success responses.
- API contract: listing response JSON gains additive fields `latitude` and `longitude`; this should be frontend-compatible for consumers that ignore unknown fields, but frontend map rendering can depend on them after contract update.
- OpenAPI/export: runtime OpenAPI and `openapi/roomhub-b2b.openapi.json` must be updated in the same implementation change.
