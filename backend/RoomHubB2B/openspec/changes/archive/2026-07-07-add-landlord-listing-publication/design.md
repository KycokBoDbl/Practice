## Context

Listing-модуль сейчас поддерживает только чтение `PUBLISHED` записей через `GET /api/listings`. Таблица `listings` уже содержит все поля объявления и nullable-ссылку `owner_organization_id`, добавленную после появления юридических лиц. JWT содержит `organizationId` и `role`, а Spring Security требует аутентификацию для всех операций вне публичного allowlist.

Изменение пересекает listing, auth/security, database constraints и OpenAPI export, но не требует нового сервиса или зависимости. Главные ограничения: нельзя доверять идентификатору владельца из request body, нельзя раскрыть публикацию роли `TENANT`, а созданная запись должна сразу удовлетворять текущему публичному фильтру `PUBLISHED`.

## Goals / Non-Goals

**Goals:**

- Реализовать атомарный `POST /api/listings` для немедленной публикации объявления ролью `LANDLORD`.
- Проверять данные на HTTP-границе и сохранять владельца, статус и время создания только из доверенного серверного контекста.
- Сохранить response contract существующего `ListingResponseDto` и видимость через `GET /api/listings`.
- Документировать и тестировать новый защищенный контракт в runtime и экспортируемом OpenAPI.

**Non-Goals:**

- Черновики, модерация, редактирование, архивирование и удаление объявлений.
- Загрузка изображений или проверка доступности внешнего URL.
- Несколько изображений, характеристики помещения сверх существующей модели и управление календарем.
- Изменение ранее загруженных объявлений без владельца.

## Decisions

### Использовать `POST /api/listings` с существующим response DTO

HTTP-метод дополняет существующий ресурс без нового landlord-specific namespace. Успешный ответ использует `ListingResponseDto` и `201 Created`, поэтому frontend получает ту же форму объявления, что и в каталоге. Альтернатива `POST /api/landlord/listings` сильнее связывает URI с ролью и дублирует ресурс; отдельный response DTO создал бы две формы одной опубликованной сущности без пользы для MVP.

### Не принимать owner, status и createdAt от клиента

Новый `CreateListingRequestDto` содержит только редактируемые поля: `title`, `description`, `city`, `address`, `pricePerHour`, `capacity`, `spaceType`, `imageUrl`. Jakarta Validation задает обязательность и ограничения, совместимые с колонками БД: непустые `title`, `city`, `address`; максимальные длины `255/100/255`; `pricePerHour > 0` с допустимой точностью; `capacity > 0`; обязательный enum; необязательные description и HTTP(S) image URL.

Service извлекает `organizationId` и `role` из JWT через listing-specific actor, требует `LANDLORD`, загружает `OrganizationEntity`, а затем создает `ListingEntity` со статусом `PUBLISHED` и временем от внедренного `Clock`. Альтернатива передавать `ownerOrganizationId` или `status` в DTO создает риск публикации от имени другой организации или обхода lifecycle.

### Сохранять публикацию одной транзакцией

Создание сущности, назначение владельца и сохранение выполняются в `@Transactional` service method. Для `ListingEntity` добавляется явный constructor/factory, который создает только полностью инициализированную публикацию; JPA no-arg constructor сохраняется. Новая append-only Flyway-миграция не добавляет полей, но закрепляет write-path инварианты: `address NOT NULL`, непустые `title`/`city`/`address`, `price_per_hour > 0`, `capacity > 0`.

### Проверять роль в доменном сервисе

Listing actor преобразует доверенные JWT claims в `organizationId` и `UserRole`, а service явно отклоняет любую роль кроме `LANDLORD` через listing-specific forbidden exception. URL остается authenticated по общему правилу `SecurityConfig`; отсутствие/невалидность токена обрабатывается resource server как `401`. Альтернатива только с `@PreAuthorize` компактнее, но оставляет service без собственного бизнес-инварианта и расходится с текущим подходом booking workflow.

### Генерировать контракт из runtime-аннотаций

Controller и DTO получают OpenAPI metadata для request body, `201`, `400`, `401`, `403` и bearer security. Contract tests проверяют операцию и схемы, после чего существующий Maven profile перегенерирует `openapi/roomhub-b2b.openapi.json`. Ручное редактирование JSON исключается, поскольку runtime остается источником истины.

Необязательные `description` и `imageUrl` помечаются nullable как во входной, так и в response schema. Ошибки используют `application/problem+json`; неизвестное enum-значение преобразуется в field-level `errors`, а прочий синтаксически поврежденный JSON остается generic malformed-body problem. Успешный ответ содержит `Location` созданного ресурса.

## Risks / Trade-offs

- [Немедленная публикация допускает контент без модерации] → ограничить MVP аутентифицированными юридическими лицами; draft/moderation оформить отдельным change.
- [Legacy-данные могут нарушать новые constraints] → миграция останавливает deployment с диагностируемой ошибкой вместо скрытого изменения данных.
- [Старые seed listings могут не иметь владельца] → не изменять их; новый write path всегда назначает владельца, сохраняя совместимость каталога.
- [Дублирование разбора JWT между booking и listing actors] → оставить bounded-context records для узкого изменения; общий security principal выделять только при появлении третьего потребителя.
- [Экспортируемый OpenAPI может отстать от runtime] → contract test плюс обязательный export и diff в той же реализации.

## Migration Plan

1. Проверить существующие listings и применить append-only migration с constraints.
2. Развернуть код с новым DTO, actor, service/controller operation и тестами.
3. Перегенерировать и закоммитить OpenAPI JSON из успешно запущенного backend.
4. Проверить smoke-сценарий: landlord публикует запись, затем анонимный `GET /api/listings` ее возвращает.
5. При rollback приложения созданные записи остаются валидными; constraints откатываются только отдельной осознанной migration.

## Open Questions

Нет блокирующих вопросов для MVP. Draft/moderation lifecycle, редактирование и media storage намеренно вынесены за рамки изменения.
