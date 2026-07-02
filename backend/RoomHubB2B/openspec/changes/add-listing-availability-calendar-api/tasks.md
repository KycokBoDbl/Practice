## 1. Схема данных

- [x] 1.1 Создать Flyway-миграцию `src/main/resources/db/migration/V4__create_listing_unavailability_periods.sql` с таблицей `listing_unavailability_periods`, колонками `start_at`/`end_at` типа `TIMESTAMP WITHOUT TIME ZONE`, FK `ON DELETE CASCADE`, проверками строгого порядка и целого часа, индексом по `listing_id`, `start_at`, `end_at`; готово, когда миграция успешно применяется к PostgreSQL, некорректные интервалы отклоняются и Hibernate validation проходит.

## 2. Доступ к данным

- [x] 2.1 Добавить `ListingUnavailabilityPeriodEntity.java` в `src/main/java/ru/esie/practice/roomhubb2b/listing/availability/` с полями `LocalDateTime startAt` и `LocalDateTime endAt`; готово, когда JPA-модель соответствует миграции и не использует `LocalDate` для границ недоступности.
- [x] 2.2 Добавить `ListingUnavailabilityPeriodRepository.java` в `src/main/java/ru/esie/practice/roomhubb2b/listing/availability/` с overlap-запросом `startAt < to AND endAt > from`; готово, когда repository одним запросом возвращает только интервалы указанного объявления с непустым пересечением полуоткрытого диапазона.
- [x] 2.3 Расширить `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingRepository.java` проверкой существования объявления по `id` и статусу `PUBLISHED`; готово, когда сервис может одинаково отсеивать отсутствующие и неопубликованные объявления без загрузки календаря.

## 3. API почасовой доступности

- [x] 3.1 Добавить DTO в `src/main/java/ru/esie/practice/roomhubb2b/listing/availability/dto/` для ответа с `listingId`, `from`, `to` и упорядоченным массивом `busyIntervals` из `{startAt, endAt}`; готово, когда Jackson сериализует локальные даты-время с точностью до часа по контракту proposal без дополнительных полей.
- [x] 3.2 Реализовать `ListingAvailabilityService.java` в `src/main/java/ru/esie/practice/roomhubb2b/listing/availability/`: проверить публикацию объявления, целый час, строгий порядок и лимит 93 дня, загрузить пересекающиеся интервалы, обрезать их по запросу, отсортировать и объединить пересекающиеся или соседние интервалы; готово, когда результат содержит нормализованные полуоткрытые интервалы, а некорректный диапазон даёт `400`.
- [x] 3.3 Реализовать `ListingAvailabilityController.java` в `src/main/java/ru/esie/practice/roomhubb2b/listing/availability/` с `GET /api/listings/{listingId}/availability` и ISO date-time параметрами `from`/`to`; готово, когда валидный запрос возвращает `200`, неверные параметры `400`, а отсутствующее или неопубликованное объявление `404`.

## 4. Автоматические тесты

- [x] 4.1 Добавить unit-тесты `src/test/java/ru/esie/practice/roomhubb2b/listing/availability/ListingAvailabilityServiceTest.java` для свободного диапазона, интервала внутри дня, интервала через полночь, обрезки, сортировки, объединения пересекающихся и соседних интервалов, касания полуоткрытых границ, нецелого часа, пустого/обратного диапазона, лимита 93 дней и `404`; готово, когда все ветви сервиса проверяются без обращения к БД.
- [x] 4.2 Добавить MVC-тесты `src/test/java/ru/esie/practice/roomhubb2b/listing/availability/ListingAvailabilityControllerTest.java` для JSON-контракта и ответов `200`, `400`, `404`; готово, когда тесты фиксируют имена полей, формат `YYYY-MM-DDTHH:00` и порядок элементов `busyIntervals`.
- [x] 4.3 Добавить repository/Flyway integration-тест `src/test/java/ru/esie/practice/roomhubb2b/listing/availability/ListingUnavailabilityPeriodRepositoryTest.java` на PostgreSQL для overlap-условия, полуоткрытых границ и check-ограничений; готово, когда тест доказывает, что касающиеся границы не пересекаются, реальные пересечения возвращаются, а нецелые часы и нестрогий порядок не сохраняются.

## 5. Итоговая проверка

- [x] 5.1 Выполнить `mvnw.cmd test` и проверить пример запроса `GET /api/listings/{id}/availability?from=2026-07-01T09:00&to=2026-07-03T18:00`; готово, когда сборка успешна, ответ соответствует spec, а существующий `GET /api/listings` продолжает работать без изменения контракта.
