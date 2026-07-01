## 1. Схема данных

- [ ] 1.1 Создать Flyway-миграцию `src/main/resources/db/migration/V6__create_booking_workflow.sql` с таблицами `bookings` и `booking_status_history`, FK, CHECK-ограничениями статусов и времени, денежными колонками и индексами; результат проверяется успешным применением миграции на PostgreSQL.
- [ ] 1.2 В той же миграции расширить `listing_unavailability_periods` колонками `source` и nullable unique `booking_id`, пометить существующие строки как `MANUAL` и добавить CHECK согласованности источника со ссылкой; результат проверяется сохранением существующих данных и невозможностью создать некорректную пару source/booking_id.
- [ ] 1.3 Добавить интеграционные проверки схемы и ограничений в `src/test/java/.../booking/BookingSchemaTest.java`; тесты должны доказать корректность enum/time/FK/source constraints.

## 2. Доменная модель и persistence

- [ ] 2.1 Добавить `BookingStatus`, `BookingEntity` и `BookingStatusHistoryEntity` в `src/main/java/ru/esie/practice/roomhubb2b/booking`; модель должна отображать все колонки, monetary precision и optimistic version.
- [ ] 2.2 Добавить `BookingRepository` и `BookingStatusHistoryRepository` с запросами для загрузки строки booking под блокировкой, хронологической истории и пакетного поиска due-состояний; repository tests должны подтверждать блокировки и порядок выборки.
- [ ] 2.3 Расширить `ListingUnavailabilityPeriodEntity` и repository поддержкой source/bookingId, overlap-запросом и удалением booking-блока; существующие availability repository tests и новые сценарии источников должны проходить.
- [ ] 2.4 Добавить pessimistic lookup объявления в `ListingRepository`, используемый как единая блокировка календарных writers; интеграционный тест должен показать сериализацию двух транзакций для одного listing.
- [ ] 2.5 Добавить конфигурируемый `Clock` и свойства 30-минутного удержания в `src/main/java/.../config` и `src/main/resources/application.properties`; unit tests должны иметь возможность подменять время без ожидания.

## 3. Создание и чтение booking

- [ ] 3.1 Реализовать в `BookingService` создание `REQUESTED` booking с проверкой опубликованного listing, будущего целочасового интервала и расчётом price snapshot; `BookingServiceTest` должен покрывать успешный расчёт и ошибки `400/404`.
- [ ] 3.2 Реализовать запись `null -> REQUESTED` и чтение стабильной хронологической истории через отдельный booking history component; тест должен подтверждать append-only порядок и отсутствие дубликатов.
- [ ] 3.3 Добавить request/response/history DTO с Jakarta Validation и фиксированным форматом `YYYY-MM-DDTHH:00` в `src/main/java/.../booking/dto`; serialization tests должны подтвердить поля и денежный формат.
- [ ] 3.4 Добавить `POST /api/bookings`, `GET /api/bookings/{bookingId}` и `GET /api/bookings/{bookingId}/history` в `BookingController`; MockMvc tests должны проверить `201`, `200`, `400` и `404` контракты.

## 4. Конечный автомат и атомарный календарь

- [ ] 4.1 Реализовать таблицу разрешённых переходов и единый метод записи status/history в `BookingService` либо отдельном `BookingStateMachine`; unit tests должны перечислить все разрешённые и запрещённые переходы.
- [ ] 4.2 Реализовать транзакционную команду approve: lock listing, повторная проверка времени/overlap, создание booking-блока, deadline и переход в `AWAITING_CONFIRMATION`; service integration tests должны покрыть свободный, занятый, соседний и уже начавшийся интервалы.
- [ ] 4.3 Реализовать reject, confirm и cancel с правилами текущего состояния и удалением calendar block при отмене удержания/будущей подтверждённой сделки; unit/integration tests должны проверить состояния, блоки и историю.
- [ ] 4.4 Сделать повтор успешных approve/confirm/reject/cancel идемпотентным без дополнительного блока и history row; тесты должны повторно вызвать каждую команду и сравнить количество записей.
- [ ] 4.5 Добавить command endpoints `/approve`, `/reject`, `/confirm`, `/cancel` в `BookingController`; MockMvc tests должны проверить успешные responses, `404` и `409`, а универсальный endpoint изменения status должен отсутствовать.
- [ ] 4.6 Добавить многопоточный PostgreSQL integration test `BookingApprovalConcurrencyTest`, запускающий два approve пересекающихся заявок; Definition of Done: ровно одна транзакция успешна, вторая получает конфликт, в календаре одна запись.

## 5. Временные переходы

- [ ] 5.1 Реализовать expiration `AWAITING_CONFIRMATION -> EXPIRED` с удалением booking-блока и защитой строки booking; тест с fixed Clock должен покрыть deadline и поздний confirm.
- [ ] 5.2 Реализовать переходы `CONFIRMED -> IN_PROGRESS -> COMPLETED`, включая обработку сразу после endAt с двумя history rows; тесты с fixed Clock должны покрыть обе границы и пропущенный цикл.
- [ ] 5.3 Добавить синхронизацию due-состояния перед чтением и командами конкретного booking; service/controller tests должны возвращать актуальное состояние без запуска scheduler.
- [ ] 5.4 Добавить пакетный scheduled processor с ограниченным размером batch и короткими транзакциями в `src/main/java/.../booking`; integration test должен обработать due bookings и не изменить ещё не наступившие.

## 6. Availability и публичный контракт

- [ ] 6.1 Проверить интеграцию booking-блоков с `ListingAvailabilityService`: `AWAITING_CONFIRMATION`/`CONFIRMED` видны как busy, а `EXPIRED`/`CANCELLED` освобождаются; добавить сценарии в `ListingAvailabilityServiceTest` и controller/repository integration tests без изменения response DTO.
- [ ] 6.2 Аннотировать booking endpoints и DTO для springdoc и расширить `OpenApiContractTests` проверками операций, request/response schemas и кодов `201/400/404/409`.
- [ ] 6.3 Обновить `openapi/roomhub-b2b.openapi.json` штатной Maven export-командой и проверить diff: существующие listing/availability schemas не изменены, booking operations добавлены.
- [ ] 6.4 Обновить `README.md` примерами create/approve/confirm/cancel, диаграммой состояний и описанием 30-минутного удержания; команды документации должны соответствовать экспортированному OpenAPI.

## 7. Итоговая проверка

- [ ] 7.1 Запустить полный `mvnw test`, исправить регрессии и убедиться, что unit, MockMvc и PostgreSQL integration tests проходят совместно.
- [ ] 7.2 Запустить приложение с PostgreSQL через `docker compose up --build`, выполнить smoke flow create -> approve -> confirm и проверить занятость через availability endpoint; зафиксировать проверяемые команды и результаты в change notes либо PR description.
