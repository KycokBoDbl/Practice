## ADDED Requirements

### Requirement: Контракт публикации объявления
Runtime OpenAPI SHALL описывать `POST /api/listings` как защищенную bearer authentication операцию публикации объявления, включая `CreateListingRequestDto`, успешный `ListingResponseDto`, nullable `description`/`imageUrl` и ответы `201`, `400`, `401` и `403`; ошибки SHALL использовать `application/problem+json`, а экспортируемый файл SHALL содержать тот же контракт.

#### Scenario: Операция публикации присутствует в runtime OpenAPI
- **WHEN** клиент читает `GET /api/openapi`
- **THEN** `paths./api/listings.post` присутствует и требует bearer security
- **AND** request body ссылается на `CreateListingRequestDto`
- **AND** ответ `201` ссылается на `ListingResponseDto`, а ответы `400`, `401` и `403` документированы как ошибки
- **AND** `description` и `imageUrl` в `ListingResponseDto` допускают `null`, а error responses описаны с media type `application/problem+json`

#### Scenario: Экспортированный контракт обновлен
- **WHEN** разработчик выполняет Maven profile `openapi-export` после добавления публикации
- **THEN** `openapi/roomhub-b2b.openapi.json` содержит тот же `POST /api/listings`, request/response schemas, security requirement и status codes, что runtime OpenAPI
