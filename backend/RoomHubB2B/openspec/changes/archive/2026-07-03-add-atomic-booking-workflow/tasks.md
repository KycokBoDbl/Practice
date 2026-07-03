## 1. Схема данных

- [x] 1.1 Создать Flyway-миграцию `src/main/resources/db/migration/V7__create_booking_workflow.sql`, выполняемую после auth migration V6: добавить nullable FK `listings.owner_organization_id -> organizations.id`, создать `bookings` с обязательным `tenant_organization_id -> organizations.id` и таблицу `booking_status_history` с FK, CHECK-ограничениями статусов и времени, денежными колонками и индексами; результат проверяется успешным применением на PostgreSQL с существующими V1-V6 данными.
- [x] 1.2 В той же миграции расширить `listing_unavailability_periods` колонками `source` и nullable unique `booking_id`, пометить существующие строки как `MANUAL` и добавить CHECK согласованности источника со ссылкой; результат проверяется сохранением существующих данных и невозможностью создать некорректную пару source/booking_id.
- [x] 1.3 Добавить интеграционные проверки схемы и ограничений в `src/test/java/.../booking/BookingSchemaTest.java`; тесты должны доказать корректность enum/time/FK/source constraints, FK организаций, допустимость legacy listing без владельца и запрет booking без tenant organization.

## 2. Доменная модель и persistence

- [x] 2.1 Добавить `BookingStatus`, `BookingEntity` и `BookingStatusHistoryEntity` в `src/main/java/ru/esie/practice/roomhubb2b/booking`, а в `ListingEntity` — внутреннюю nullable-связь владельца с `OrganizationEntity`; booking model должна отображать tenant organization, все колонки, monetary precision и optimistic version, не меняя `ListingResponseDto`.
- [x] 2.2 Добавить `BookingRepository` и `BookingStatusHistoryRepository` с ownership-aware запросами для участника, загрузки строки booking под блокировкой, хронологической истории и пакетного поиска due-состояний; repository tests должны подтверждать блокировки, tenant/landlord visibility и порядок выборки.
- [x] 2.3 Расширить `ListingUnavailabilityPeriodEntity` и repository поддержкой source/bookingId, overlap-запросом и удалением booking-блока; существующие availability repository tests и новые сценарии источников должны проходить.
- [x] 2.4 Добавить pessimistic lookup объявления в `ListingRepository`, используемый как единая блокировка календарных writers; интеграционный тест должен показать сериализацию двух транзакций для одного listing.
- [x] 2.5 Добавить конфигурируемый `Clock` и свойства 30-минутного удержания в `src/main/java/.../config` и `src/main/resources/application.properties`; unit tests должны иметь возможность подменять время без ожидания.

## 3. Создание и чтение booking

- [x] 3.1 Реализовать в `BookingService` создание `REQUESTED` booking только для `ROLE_TENANT`: tenant organization брать из проверенного JWT principal, проверять опубликованный listing с назначенным landlord owner, будущий целочасовой интервал и рассчитывать price snapshot; request DTO не должен принимать participant ids, а `BookingServiceTest` должен покрывать успешный расчёт и ошибки `400/403/404`.
- [x] 3.2 Реализовать запись `null -> REQUESTED` и чтение стабильной хронологической истории через отдельный booking history component; тест должен подтверждать append-only порядок и отсутствие дубликатов.
- [x] 3.3 Добавить request/response/history DTO с Jakarta Validation и фиксированным форматом `YYYY-MM-DDTHH:00` в `src/main/java/.../booking/dto`; serialization tests должны подтвердить поля и денежный формат.
- [x] 3.4 Добавить `POST /api/bookings`, `GET /api/bookings/{bookingId}` и `GET /api/bookings/{bookingId}/history` в `BookingController` с `@AuthenticationPrincipal Jwt` либо эквивалентным principal; MockMvc tests должны проверить `201`, `200`, `400`, `401`, `403`, доступ обоих участников и маскирующий `404` для чужой организации.
- [x] 3.5 Добавить отдельные authorization tests: `TENANT` не может создавать booking от имени другой организации, `LANDLORD` не может создавать booking, посторонняя организация не читает booking/history, а отсутствие или повреждение token обрабатывается существующим auth `ProblemDetail`.

## 4. Конечный автомат и атомарный календарь

- [x] 4.1 Реализовать таблицу разрешённых переходов и единый метод записи status/history в `BookingService` либо отдельном `BookingStateMachine`; unit tests должны перечислить все разрешённые и запрещённые переходы.
- [x] 4.2 Реализовать транзакционную команду approve только для `ROLE_LANDLORD` организации-владельца listing: ownership check, lock listing, повторная проверка времени/overlap, создание booking-блока, deadline и переход в `AWAITING_CONFIRMATION`; service integration tests должны покрыть владельца, чужого landlord, неверную роль, свободный, занятый, соседний и уже начавшийся интервалы.
- [x] 4.3 Реализовать reject только для landlord owner, а confirm и cancel только для tenant organization booking, вместе с правилами текущего состояния и удалением calendar block при отмене удержания/будущей подтверждённой сделки; unit/integration tests должны проверить ownership, состояния, блоки и историю.
- [x] 4.4 Сделать повтор успешных approve/confirm/reject/cancel идемпотентным без дополнительного блока и history row; тесты должны повторно вызвать каждую команду и сравнить количество записей.
- [x] 4.5 Добавить защищённые command endpoints `/approve`, `/reject`, `/confirm`, `/cancel` в `BookingController`; MockMvc tests должны проверить успешные responses, `401`, `403`, маскирующий `404`, `409`, bearer security requirement и отсутствие универсального endpoint изменения status.
- [x] 4.6 Добавить многопоточный PostgreSQL integration test `BookingApprovalConcurrencyTest`, запускающий два approve пересекающихся заявок; Definition of Done: ровно одна транзакция успешна, вторая получает конфликт, в календаре одна запись.

## 5. Временные переходы

- [x] 5.1 Реализовать expiration `AWAITING_CONFIRMATION -> EXPIRED` с удалением booking-блока и защитой строки booking; тест с fixed Clock должен покрыть deadline и поздний confirm.
- [x] 5.2 Реализовать переходы `CONFIRMED -> IN_PROGRESS -> COMPLETED`, включая обработку сразу после endAt с двумя history rows; тесты с fixed Clock должны покрыть обе границы и пропущенный цикл.
- [x] 5.3 Добавить синхронизацию due-состояния перед чтением и командами конкретного booking; service/controller tests должны возвращать актуальное состояние без запуска scheduler.
- [x] 5.4 Добавить пакетный scheduled processor с ограниченным размером batch и короткими транзакциями в `src/main/java/.../booking`; integration test должен обработать due bookings и не изменить ещё не наступившие.

## 6. Availability и публичный контракт

- [x] 6.1 Проверить интеграцию booking-блоков с `ListingAvailabilityService`: `AWAITING_CONFIRMATION`/`CONFIRMED` видны как busy, а `EXPIRED`/`CANCELLED` освобождаются; добавить сценарии в `ListingAvailabilityServiceTest` и controller/repository integration tests без изменения response DTO.
- [x] 6.2 Аннотировать booking endpoints и DTO для springdoc и расширить `OpenApiContractTests` проверками bearer security requirement, операций, request/response schemas и кодов `201/400/401/403/404/409`.
- [x] 6.3 Обновить `openapi/roomhub-b2b.openapi.json` штатной Maven export-командой и проверить diff: схема `ListingResponseDto` и availability schemas не изменены, booking operations защищены и добавлены.
- [x] 6.4 Обновить `README.md` примерами регистрации/login двух ролей и create/approve/confirm/cancel с bearer tokens, диаграммой состояний и описанием 30-минутного удержания; команды документации должны соответствовать экспортированному OpenAPI.

## 7. Итоговая проверка

- [x] 7.1 Запустить полный `mvnw test`, исправить регрессии и убедиться, что unit, MockMvc и PostgreSQL integration tests проходят совместно.
- [x] 7.2 Запустить приложение с PostgreSQL через `docker compose up --build`, зарегистрировать `TENANT` и `LANDLORD`, подготовить owned listing fixture без изменения публичного listing API, выполнить с их bearer tokens smoke flow create -> approve -> confirm и проверить занятость через availability endpoint; дополнительно проверить `403` для неверной роли и `404` для чужой организации.
