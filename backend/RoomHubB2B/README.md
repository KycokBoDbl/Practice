# RoomHub B2B Backend

Backend маркетплейса краткосрочной аренды коммерческих помещений. Приложение запускается на порту `8081`; опубликованные объявления доступны через `GET /api/listings`.

## Требования

- Java 17 или новее;
- PostgreSQL с доступной базой данных `roomhub_b2b`;
- Maven Wrapper из репозитория (`mvnw.cmd` или `mvnw`).

По умолчанию приложение использует:

```text
jdbc:postgresql://localhost:5432/roomhub_b2b
username: postgres
password: postgres
```

Для другой базы задайте переменные окружения:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/roomhub_b2b"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "postgres"
$env:ROOMHUB_AUTH_TOKEN_SECRET = "replace-with-at-least-32-random-bytes"
$env:ROOMHUB_GEOCODING_YANDEX_API_KEY = "replace-with-yandex-geocoder-api-key"
$env:ROOMHUB_AI_GIGACHAT_AUTHORIZATION_KEY = "replace-with-gigachat-authorization-key"
```

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/roomhub_b2b
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export ROOMHUB_AUTH_TOKEN_SECRET=replace-with-at-least-32-random-bytes
export ROOMHUB_GEOCODING_YANDEX_API_KEY=replace-with-yandex-geocoder-api-key
export ROOMHUB_AI_GIGACHAT_AUTHORIZATION_KEY=replace-with-gigachat-authorization-key
```

## Запуск

`ROOMHUB_GEOCODING_YANDEX_API_KEY` is required and supplies the Yandex Geocoder API key used to geocode listing `city + address` on create and address edit. Set it from secret storage in production and shared environments.

`ROOMHUB_AI_GIGACHAT_AUTHORIZATION_KEY` is required and supplies the Basic authorization key used to obtain a GigaChat access token for AI listing search. Store it as a secret. Optional GigaChat settings are `GIGACHAT_OAUTH_URL`, `GIGACHAT_CHAT_URL`, `GIGACHAT_MODEL`, `GIGACHAT_SCOPE`, `GIGACHAT_TIMEOUT`, `GIGACHAT_MAX_PROMPT_LENGTH`, `GIGACHAT_DEFAULT_LISTING_LIMIT`, and `GIGACHAT_MAX_LISTING_LIMIT`. AI search does not require a database migration.

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

`ROOMHUB_AUTH_TOKEN_SECRET` обязателен и должен содержать не менее 32 байт UTF-8. Значение выше служит только placeholder; для окружения используйте случайный секрет из secret storage. Issuer и TTL можно переопределить через `ROOMHUB_AUTH_TOKEN_ISSUER` и `ROOMHUB_AUTH_TOKEN_TTL` (ISO-8601 duration), по умолчанию используются `roomhub-b2b` и `PT15M`.

## AI Listing Search

Frontend clients can submit a natural-language prompt and render the returned listing cards with the existing `ListingResponseDto` schema.

```http
POST /api/listings/ai-search
Content-Type: application/json

{
  "prompt": "Need a conference hall in Barnaul for 30 people under 5000 per hour"
}
```

Successful response:

```json
[
  {
    "id": 42,
    "title": "Conference hall",
    "city": "Barnaul",
    "pricePerHour": 4500.00,
    "capacity": 40,
    "spaceType": "CONFERENCE_HALL",
    "imageUrl": "https://example.com/listing.jpg",
    "description": "Projector and reception area",
    "address": "Lenina Avenue, 10",
    "ownerOrganizationName": "Landlord LLC",
    "latitude": 53.348114,
    "longitude": 83.779836
  }
]
```

The endpoint is public. It returns `400 Bad Request` for invalid prompt JSON or blank/too-long prompts, `502 Bad Gateway` when GigaChat token/model integration fails or returns invalid filter JSON, and `200 OK` with `[]` when no published listings match.

## Аутентификация юридического лица

Регистрация создаёт одну организацию и одного основного пользователя с ролью `LANDLORD` или `TENANT`:

```http
POST /api/auth/register
Content-Type: application/json

{
  "role": "LANDLORD",
  "legalName": "ООО Деловой центр",
  "taxId": "2225123456",
  "email": "owner@example.com",
  "password": "S3cure-roomhub-password"
}
```

Успешный ответ имеет статус `201` и содержит `userId`, `organizationId`, `role`, `legalName`, `taxId` и `email`, но не содержит пароль или его hash.

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "owner@example.com",
  "password": "S3cure-roomhub-password"
}
```

Ответ `200` содержит `accessToken`, `tokenType: "Bearer"` и `expiresIn: 900`. Текущий профиль доступен по token:

```http
GET /api/auth/me
Authorization: Bearer <accessToken>
```

Регистрация и вход, каталог, availability и `/api/openapi` публичны; остальные application endpoints требуют bearer token. MVP не включает проверку организации через ФНС/ЕГРЮЛ, подтверждение email, восстановление пароля, refresh token, logout, отзыв отдельных token и несколько пользователей или ролей в организации.

## Бронирование

Booking API использует две зарегистрированные организации. `TENANT` создаёт, подтверждает и отменяет собственную заявку; `LANDLORD` может одобрить или отклонить только заявку на принадлежащее его организации объявление. Идентификаторы участников берутся из JWT и не передаются в request body.

Публичного API создания и назначения владельца объявления пока нет. Для локального smoke flow назначьте существующий listing зарегистрированной организации-арендодателю операционной SQL-командой:

```sql
UPDATE listings
SET owner_organization_id = :landlordOrganizationId
WHERE id = :listingId;
```

Создание заявки:

```http
POST /api/bookings
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json

{
  "listingId": 42,
  "startAt": "2030-07-10T10:00",
  "endAt": "2030-07-10T13:00"
}
```

Арендодатель удерживает свободный слот на 30 минут, но не позже начала аренды:

```http
POST /api/bookings/81/approve
Authorization: Bearer <landlordAccessToken>
```

Арендатор подтверждает либо отменяет сделку:

```http
POST /api/bookings/81/confirm
Authorization: Bearer <tenantAccessToken>
```

```http
POST /api/bookings/81/cancel
Authorization: Bearer <tenantAccessToken>
```

Переходы состояния:

```text
REQUESTED --approve--> AWAITING_CONFIRMATION --confirm--> CONFIRMED
    |                         |                           |
    +--reject--> REJECTED     +--expire--> EXPIRED       +--start--> IN_PROGRESS
    |                         |                           |               |
    +--cancel--> CANCELLED    +--cancel--> CANCELLED     +--cancel       +--finish--> COMPLETED
                                                          before start
```

Одобрение атомарно проверяет общий календарь `listing_unavailability_periods`. При двух конкурентных заявках на пересекающееся время слот получает только одна; вторая получает `409 Conflict`. Booking и его история доступны только tenant organization сделки и landlord organization исходного listing.

## OpenAPI в runtime

Запущенный backend отдаёт актуальный OpenAPI 3 JSON:

```text
http://localhost:8081/api/openapi
```

Проверка:

```powershell
Invoke-RestMethod http://localhost:8081/api/openapi
```

```bash
curl --fail http://localhost:8081/api/openapi
```

Swagger UI не включён. Источником контракта являются Spring MVC mappings, DTO и OpenAPI-аннотации backend.

## Экспорт OpenAPI

Экспорт запускает тесты, поднимает backend на временном порту `18081`, получает runtime-контракт, сохраняет его в `openapi/roomhub-b2b.openapi.json` и останавливает backend. PostgreSQL должен быть доступен с указанными выше настройками.

Windows:

```powershell
.\mvnw.cmd verify -Popenapi-export
```

Linux/macOS:

```bash
./mvnw verify -Popenapi-export
```

При конфликте портов переопределите HTTP- и JMX-порты экспорта:

```powershell
.\mvnw.cmd "-Dopenapi.export.port=18082" "-Dopenapi.export.jmx-port=19002" verify -Popenapi-export
```

```bash
./mvnw -Dopenapi.export.port=18082 -Dopenapi.export.jmx-port=19002 verify -Popenapi-export
```

Команда завершится с ошибкой, если приложение не запустится или `/api/openapi` нельзя получить. Generated-файл не редактируется вручную.

## Регулярный pipeline API-контракта

Для изменения endpoint, параметра, HTTP status или DTO:

1. Обновите controller, DTO, validation и OpenAPI-аннотации в одном изменении.
2. Запустите тесты: `.\mvnw.cmd test` или `./mvnw test`.
3. Обновите контракт: `.\mvnw.cmd verify -Popenapi-export` или `./mvnw verify -Popenapi-export`.
4. Проверьте `git diff -- openapi/roomhub-b2b.openapi.json`. Diff должен отражать только намеренное изменение публичного API.
5. Закоммитьте backend-код и `openapi/roomhub-b2b.openapi.json` вместе.
6. Передайте frontend-разработчику generated-файл или используйте его как input для принятого в frontend генератора типов/API-клиента.

Для внутреннего изменения без влияния на API выполните те же тесты и export. Отсутствие diff в generated-файле подтверждает, что frontend-контракт не изменился.

Docker build запускает автономные unit/controller-тесты, которым не нужна внешняя база данных. Полный набор тестов использует PostgreSQL и должен быть успешно выполнен командой `./mvnw test` до `docker compose up --build`.

Проверка drift для CI или перед commit:

```powershell
.\mvnw.cmd verify -Popenapi-export
git diff --exit-code -- openapi/roomhub-b2b.openapi.json
```

```bash
./mvnw verify -Popenapi-export
git diff --exit-code -- openapi/roomhub-b2b.openapi.json
```
