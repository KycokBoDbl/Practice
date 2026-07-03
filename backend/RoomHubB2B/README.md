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
```

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/roomhub_b2b
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export ROOMHUB_AUTH_TOKEN_SECRET=replace-with-at-least-32-random-bytes
```

## Запуск

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

`ROOMHUB_AUTH_TOKEN_SECRET` обязателен и должен содержать не менее 32 байт UTF-8. Значение выше служит только placeholder; для окружения используйте случайный секрет из secret storage. Issuer и TTL можно переопределить через `ROOMHUB_AUTH_TOKEN_ISSUER` и `ROOMHUB_AUTH_TOKEN_TTL` (ISO-8601 duration), по умолчанию используются `roomhub-b2b` и `PT15M`.

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
