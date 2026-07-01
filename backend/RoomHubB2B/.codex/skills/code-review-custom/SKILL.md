---
name: code-review-custom
description: Кастомный скилл для анализа кодовой базы
---

# RoomHubB2B Backend Code Review Skill

## Назначение

Используй эту инструкцию при code review backend-части приложения **RoomHubB2B**.

Проект представляет собой backend на Spring Boot для B2B-сервиса аренды помещений. Основные доменные области:

* listings — объявления о помещениях;
* availability — доступность и периоды недоступности помещений;
* bookings — бронирования;
* users / organizations — пользователи, владельцы, арендаторы, компании;
* moderation — модерация объявлений;
* payments — платежи;
* reviews — отзывы.

Цель code review — не только найти синтаксические ошибки, но и проверить, что код остаётся чистым, расширяемым, безопасным и готовым к развитию MVP.

---

## Роль ревьюера

При ревью действуй как backend reviewer уровня middle+/senior.

Проверяй код с точки зрения:

1. архитектуры;
2. чистоты кода;
3. доменной модели;
4. REST API;
5. безопасности;
6. работы с БД;
7. миграций Flyway;
8. тестируемости;
9. обработки ошибок;
10. поддержки OpenAPI;
11. production-readiness.

Не ограничивайся косметическими замечаниями. В первую очередь ищи архитектурные, доменные и эксплуатационные риски.

---

## Формат ответа при code review

Ответ должен быть структурирован так:


## Общая оценка

Кратко оцени качество изменений и основные риски.

## Что сделано хорошо

Перечисли сильные стороны кода.

## Критичные замечания

Замечания, которые нужно исправить до merge.

## Важные замечания

Проблемы, которые не блокируют merge полностью, но требуют исправления в ближайшее время.

## Минорные замечания

Стиль, читаемость, небольшие улучшения.

## Рекомендации по тестам

Какие тесты нужно добавить или обновить.

## Итог

Чёткий verdict:
- approve;
- approve with comments;
- request changes.


Для каждого замечания указывай:


- где проблема;
- почему это проблема;
- какой риск она создаёт;
- как лучше исправить;
- пример исправления, если это полезно.


---

# 1. Архитектура backend

## 1.1. Проверяй разделение слоёв

Backend должен быть организован по слоям:


Controller -> Service -> Repository -> Database
          DTO        Entity


### Правило

Контроллеры не должны содержать бизнес-логику.

Controller должен только:

1. принимать HTTP-запрос;
2. валидировать входные параметры;
3. вызывать service;
4. возвращать DTO;
5. описывать endpoint через OpenAPI-аннотации, если это принято в проекте.

### Нарушение

@GetMapping("/api/listings")
public List<ListingResponseDto> getListings() {
    return listingRepository.findByStatus("PUBLISHED").stream()
            .filter(listing -> listing.getCapacity() > 0)
            .map(...)
            .toList();
}


### Как должно быть

@GetMapping("/api/listings")
public Page<ListingCardDto> getListings(ListingSearchRequest request, Pageable pageable) {
    return listingService.getPublishedListings(request, pageable);
}


---

## 1.2. Проверяй, что бизнес-логика находится в Service

Service должен содержать use-case logic:

* получить опубликованные объявления;
* проверить доступность помещения;
* создать бронирование;
* отменить бронирование;
* отправить объявление на модерацию;
* опубликовать объявление;
* проверить права владельца.

Если бизнес-правило находится в controller, repository, mapper или entity без веской причины — это замечание.

---

## 1.3. Проверяй, что Repository отвечает только за доступ к данным

Repository не должен содержать бизнес-решения.

Допустимо:

Page<ListingEntity> findByStatus(ListingStatus status, Pageable pageable);

boolean existsByIdAndStatus(Long id, ListingStatus status);


Подозрительно:

List<ListingEntity> findAvailablePublishedListingsForTenantWithValidPriceAndCapacity(...);


Если query-метод становится слишком длинным и описывает бизнес-сценарий, предложи вынести бизнес-логику в service/domain layer.

---

## 1.4. Проверяй domain-based package structure

Предпочтительная структура пакетов:


ru.esie.practice.roomhubb2b
 ├── listing
 │   ├── ListingController
 │   ├── ListingService
 │   ├── ListingRepository
 │   ├── ListingEntity
 │   ├── ListingMapper
 │   └── dto
 │
 ├── booking
 │   ├── BookingController
 │   ├── BookingService
 │   ├── BookingRepository
 │   ├── BookingEntity
 │   └── dto
 │
 ├── availability
 │   ├── ListingAvailabilityController
 │   ├── ListingAvailabilityService
 │   └── dto
 │
 ├── common
 │   ├── error
 │   ├── config
 │   └── security


Пакеты должны отражать домен, а не быть свалкой технических классов.

---

# 2. DTO, Entity и API-контракты

## 2.1. Entity нельзя возвращать из API

### Правило

Контроллеры не должны возвращать JPA Entity напрямую.

Нельзя:

@GetMapping("/api/listings")
public List<ListingEntity> getListings() {
    return listingRepository.findAll();
}


Нужно:

@GetMapping("/api/listings")
public Page<ListingCardDto> getListings(Pageable pageable) {
    return listingService.getPublishedListings(pageable);
}


### Почему

Entity — это модель базы данных. DTO — это контракт API.

Риски при возврате Entity:

* случайная утечка внутренних полей;
* жёсткая связка API со схемой БД;
* проблемы с lazy loading;
* непредсказуемая JSON-сериализация;
* сложнее версионировать API.

---

## 2.2. DTO должны быть специализированными

Для RoomHubB2B не должно быть одного универсального `ListingDto` на все случаи.

Предпочтительно:


ListingCardDto — карточка объявления в списке;
ListingDetailsDto — подробная страница объявления;
ListingCreateRequest — создание объявления;
ListingUpdateRequest — редактирование объявления;
ListingSearchRequest — фильтры поиска;
ListingAvailabilityResponse — доступность помещения;
BookingCreateRequest — создание бронирования;
BookingResponseDto — данные бронирования.


Если один DTO используется и для списка, и для деталей, и для создания — это повод для замечания.

---

## 2.3. DTO должны быть immutable

Для Java 17+ предпочтительно использовать `record`.

Хорошо:

public record ListingCardDto(
        Long id,
        String title,
        String city,
        BigDecimal pricePerHour,
        Integer capacity,
        SpaceType spaceType,
        String imageUrl
) {}


Mutable DTO с setters допустимы только при наличии причины.

---

## 2.4. Entity не должна содержать DTO-логику

Нельзя:

@Entity
public class ListingEntity {

    public ListingResponseDto toDto() {
        return new ListingResponseDto(...);
    }
}


Нужно вынести маппинг в mapper:

@Component
public class ListingMapper {

    public ListingCardDto toCardDto(ListingEntity listing) {
        return new ListingCardDto(...);
    }
}


---

# 3. Доменные значения и enum

## 3.1. Не использовать строки для статусов и типов

Нельзя использовать строковые литералы:

listingRepository.findByStatus("PUBLISHED");


Нужно использовать enum:

listingRepository.findByStatus(ListingStatus.PUBLISHED);


Пример:

public enum ListingStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
    BLOCKED
}


public enum SpaceType {
    MEETING_ROOM,
    COWORKING,
    EVENT_HALL,
    OFFICE,
    STUDIO
}


В entity:

@Enumerated(EnumType.STRING)
@Column(nullable = false)
private ListingStatus status;


---

## 3.2. Проверяй наличие DB constraints для enum

Если в Java есть enum, в PostgreSQL должна быть защита на уровне БД.

Пример:

sql
CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'BLOCKED'))


sql
CHECK (space_type IN ('MEETING_ROOM', 'COWORKING', 'EVENT_HALL', 'OFFICE', 'STUDIO'))


---

# 4. Валидация входных данных

## 4.1. Request DTO должны валидироваться через Bean Validation

Для входных DTO должны использоваться:

@NotNull
@NotBlank
@Size
@Positive
@PositiveOrZero
@Min
@Max
@Email
@Pattern
@Future
@FutureOrPresent


Пример:

public record ListingCreateRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 2000)
        String description,

        @NotBlank
        @Size(max = 100)
        String city,

        @NotBlank
        @Size(max = 255)
        String address,

        @NotNull
        @Positive
        BigDecimal pricePerHour,

        @NotNull
        @Positive
        Integer capacity,

        @NotNull
        SpaceType spaceType
) {}


В controller обязательно:

@PostMapping("/api/listings")
public ListingDetailsDto createListing(@Valid @RequestBody ListingCreateRequest request) {
    return listingService.createListing(request);
}


---

## 4.2. Бизнес-валидация должна быть явной

Пример для availability:

if (!from.isBefore(to)) {
    throw new InvalidAvailabilityRangeException(from, to);
}


Проверяй наличие валидации:

* `from < to`;
* диапазон не превышает максимальный лимит;
* время соответствует шагу бронирования;
* помещение существует;
* помещение опубликовано;
* пользователь имеет право на действие;
* цена положительная;
* вместимость положительная.

---

# 5. Ошибки и исключения

## 5.1. Все ошибки API должны иметь единый формат

Нельзя полагаться на случайный default error Spring Boot.

Предпочтительный формат:

public record ApiErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {}


Пример ответа:

json
{
  "code": "LISTING_NOT_FOUND",
  "message": "Listing was not found or is not published.",
  "details": {
    "listingId": 42
  }
}


---

## 5.2. Должен быть глобальный exception handler

Проверяй наличие:

@RestControllerAdvice
public class ApiExceptionHandler {
}


Он должен обрабатывать:

* custom business exceptions;
* validation errors;
* `MethodArgumentNotValidException`;
* `ConstraintViolationException`;
* type mismatch errors;
* not found errors;
* access denied errors;
* unexpected errors.

---

## 5.3. Не злоупотреблять ResponseStatusException

Для прототипа допустимо:

throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found");


Для чистого backend-кода лучше:

throw new ListingNotFoundException(listingId);


HTTP-статус должен назначаться в `ApiExceptionHandler`, а не размазываться по бизнес-логике.

---

## 5.4. Не использовать generic RuntimeException для бизнес-ошибок

Нельзя:

throw new RuntimeException("Listing not found");


Нужно:

throw new ListingNotFoundException(listingId);


или:

throw new InvalidAvailabilityRangeException(from, to);


---

# 6. REST API

## 6.1. Endpoints должны быть REST-предсказуемыми

Хорошо:


GET    /api/v1/listings
GET    /api/v1/listings/{listingId}
POST   /api/v1/listings
PUT    /api/v1/listings/{listingId}
DELETE /api/v1/listings/{listingId}

GET    /api/v1/listings/{listingId}/availability

POST   /api/v1/bookings
GET    /api/v1/bookings/{bookingId}
POST   /api/v1/bookings/{bookingId}/cancel


Плохо:


GET  /api/getListings
POST /api/createBooking
POST /api/listing/update
GET  /api/checkAvailabilityForListing


Глаголы должны быть в HTTP-методах, а не в URL.

---

## 6.2. Коллекции должны иметь пагинацию

Любой endpoint, возвращающий список, должен поддерживать пагинацию.

Плохо:

List<ListingResponseDto> getListings()


Хорошо:

Page<ListingCardDto> getListings(ListingSearchRequest request, Pageable pageable)


Исключения допустимы только для маленьких справочников:


GET /api/v1/space-types
GET /api/v1/listing-statuses


---

## 6.3. Фильтрация должна быть явной

Для `/api/v1/listings` ожидаемые фильтры:


city
spaceType
minCapacity
maxPricePerHour
from
to
page
size
sort


Фильтры лучше оформлять отдельным request object:

public record ListingSearchRequest(
        String city,
        SpaceType spaceType,
        Integer minCapacity,
        BigDecimal maxPricePerHour
) {}


---

## 6.4. Не раскрывать лишние данные

Проверяй, не отдаёт ли public API чувствительные или внутренние поля:

* внутренние IDs владельцев;
* email владельца;
* телефон владельца;
* технические статусы;
* полные адреса, если бизнес-логика не предполагает публичный показ;
* служебные timestamps;
* audit-поля.

Для списка объявлений лучше использовать краткий DTO без лишних данных.

---

# 7. Availability и работа со временем

## 7.1. Время должно быть timezone-aware

RoomHubB2B работает с помещениями и бронированиями, поэтому время — критичная часть домена.

Проверяй, нет ли неявного использования:

LocalDateTime.now()


или неясного `LocalDateTime` без timezone-правила.

Предпочтительно:

* в БД хранить `timestamptz` / `Instant`;
* в API использовать `OffsetDateTime`;
* у помещения хранить timezone;
* в бизнес-логике учитывать timezone помещения.

Если используется `LocalDateTime`, должно быть явно зафиксировано правило:


Все даты availability передаются в локальном времени помещения.
У каждого помещения есть timezone.


В `listings` желательно поле:

sql
timezone VARCHAR(64) NOT NULL


Примеры значений:


Europe/Amsterdam
Europe/Berlin
Asia/Almaty


---

## 7.2. Не использовать системное время напрямую

Плохо:

LocalDateTime now = LocalDateTime.now();


Лучше:

Instant now = clock.instant();


`Clock` должен внедряться как bean:

@Bean
public Clock clock() {
    return Clock.systemUTC();
}


Это упрощает тестирование.

---

## 7.3. Availability должна иметь понятный контракт

Проверяй, что endpoint ясно говорит, что он возвращает:

* доступные интервалы;
* занятые интервалы;
* периоды недоступности;
* бронирования;
* объединённые интервалы занятости.

Нельзя смешивать `availability`, `unavailability` и `booking` без явной модели.

Хорошие названия:


busyIntervals
availableIntervals
unavailablePeriods
bookingSlots


Плохие названия:


data
periods
items
times


---

## 7.4. Проверяй edge cases для интервалов

Для availability обязательно должны быть тесты на:


from == to
from > to
minutes != 00
seconds != 00
range > max allowed range
listing not found
listing exists but not published
overlapping periods
adjacent periods
period starts before requested range
period ends after requested range
period fully inside requested range
period fully outside requested range


---

# 8. База данных и Flyway

## 8.1. Инварианты должны защищаться на уровне БД

Java-валидации недостаточно. В PostgreSQL должны быть constraints.

Для `listings`:

sql
CHECK (price_per_hour > 0)
CHECK (capacity > 0)
CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'BLOCKED'))
CHECK (space_type IN ('MEETING_ROOM', 'COWORKING', 'EVENT_HALL', 'OFFICE', 'STUDIO'))


Для availability:

sql
CHECK (end_at > start_at)


Если бронирования не могут пересекаться, желательно использовать exclusion constraint или другую защиту от overlapping bookings.

---

## 8.2. Миграции Flyway нельзя редактировать после применения

Правила:


Не редактировать уже применённые миграции.
Каждая миграция делает одну логическую вещь.
Название миграции должно объяснять смысл.
Demo seed data не должен попадать в production migrations.


Хорошо:


V1__create_listings_table.sql
V2__create_listing_unavailability_periods.sql
V3__add_listing_timezone.sql
V4__add_listing_status_check_constraint.sql


Плохо:


V5__fix.sql
V6__changes.sql
V7__new_update.sql


---

## 8.3. JPA-модель должна соответствовать схеме БД

Если в БД поле `NOT NULL`, в entity должно быть:

@Column(nullable = false)


Если в БД ограничение длины, в entity должно быть:

@Column(length = 255)


Если поле денежное:

@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal pricePerHour;


---

## 8.4. Для денежных значений использовать BigDecimal

Нельзя использовать `double` или `float` для цены.

Плохо:

private double pricePerHour;


Хорошо:

private BigDecimal pricePerHour;


---

# 9. Security

## 9.1. Проверяй наличие авторизации для mutating endpoints

Все endpoints, которые меняют данные, должны быть защищены:


POST
PUT
PATCH
DELETE


Примеры:


POST /api/v1/listings
PUT /api/v1/listings/{listingId}
DELETE /api/v1/listings/{listingId}
POST /api/v1/bookings
POST /api/v1/bookings/{bookingId}/cancel
POST /api/v1/admin/listings/{listingId}/approve


---

## 9.2. Роли должны быть явными

Ожидаемые роли:


TENANT
OWNER
ADMIN


Права должны быть централизованы.

Хорошо:

@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/api/admin/listings/{id}/approve")
public void approveListing(@PathVariable Long id) {
    moderationService.approveListing(id);
}


Для владельца:

@PreAuthorize("@listingSecurity.isOwner(#listingId, authentication)")
@PutMapping("/api/owner/listings/{listingId}")
public ListingDetailsDto updateListing(...) {
    return listingService.updateListing(...);
}


Плохо:

if (!user.getRole().equals("ADMIN")) {
    throw new RuntimeException();
}


---

## 9.3. Не логировать чувствительные данные

Нельзя логировать:


пароли;
токены;
refresh tokens;
платёжные данные;
персональные данные без необходимости;
полные authorization headers.


---

## 9.4. CORS должен быть конфигурируемым

Плохо:

configuration.setAllowedOrigins(List.of("http://localhost:5173"));


Лучше:

properties
roomhub.cors.allowed-origins=http://localhost:5173


Через `@ConfigurationProperties`.

---

# 10. Конфигурация

## 10.1. Не хардкодить environment-specific значения

Нельзя хардкодить:


DB URL
DB username
DB password
frontend URL
JWT secret
payment provider keys
external service URLs


Они должны приходить из:


environment variables
application-dev.properties
application-test.properties
application-prod.properties
@ConfigurationProperties


---

## 10.2. Production defaults должны быть безопасными

В production не должно быть:

properties
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=create
spring.jpa.hibernate.ddl-auto=update


Рекомендуемо:

properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false


---

# 11. OpenAPI

## 11.1. Public endpoints должны быть описаны

Для каждого public endpoint проверяй:

* есть summary;
* есть description, если endpoint неочевидный;
* описаны успешные ответы;
* описаны ошибки;
* описаны request parameters;
* описаны request/response DTO;
* error response соответствует общему формату.

Пример:

@Operation(summary = "Get published listings")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Published listings returned"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters")
})
@GetMapping("/api/v1/listings")
public Page<ListingCardDto> getListings(...) {
    ...
}


---

## 11.2. OpenAPI-контракт должен обновляться осознанно

Если изменился endpoint, DTO или формат ошибки, проверь, обновляется ли OpenAPI.

Если в проекте есть тест на OpenAPI contract, он должен проходить.

---

# 12. Тесты

## 12.1. Новая бизнес-логика должна иметь unit tests

Service-логика должна тестироваться отдельно.

Особенно важно тестировать:


availability calculations
booking creation
booking cancellation
listing publication
moderation decisions
permission checks
price/capacity validation


---

## 12.2. Новые endpoints должны иметь controller tests

Controller tests должны проверять:


HTTP method
URL
query params
request body validation
status codes
response body
error response body
authorization behavior


---

## 12.3. Repository tests нужны для сложных запросов

Если repository содержит custom query, нужен тест.

Особенно для:


overlapping availability periods
booking conflicts
filtering listings
pagination queries
moderation queues


---

## 12.4. Тесты должны проверять поведение, а не реализацию

Плохо:

verify(repository).findByStatus(ListingStatus.PUBLISHED);


Лучше:

assertThat(result.getContent())
        .extracting(ListingCardDto::status)
        .containsOnly(ListingStatus.PUBLISHED);


---

## 12.5. Тестовые данные должны быть читаемыми

Для сложных тестов использовать test data builders.

Хорошо:

ListingEntity listing = listing()
        .published()
        .city("Amsterdam")
        .capacity(10)
        .pricePerHour("100.00")
        .build();


Плохо:

ListingEntity listing = new ListingEntity();
listing.setTitle("Room 1");
listing.setCity("Amsterdam");
listing.setPricePerHour(new BigDecimal("100.00"));
listing.setCapacity(10);
listing.setStatus(ListingStatus.PUBLISHED);
listing.setSpaceType(SpaceType.MEETING_ROOM);


---

# 13. Чистота кода

## 13.1. Имена должны объяснять бизнес-смысл

Хорошо:

getPublishedListings
getListingAvailability
createBooking
cancelBooking
approveListing
normalizeBusyIntervals


Плохо:

getData
process
handle
check
doWork
manage


---

## 13.2. Один метод — одна ответственность

Если метод делает сразу:


валидацию;
запрос в БД;
маппинг;
расчёты;
проверку прав;
логирование;
создание ответа;


его нужно разбить.

Хорошо:

public ListingAvailabilityResponse getAvailability(Long listingId, LocalDateTime from, LocalDateTime to) {
    validateAvailabilityRange(from, to);

    ListingEntity listing = getPublishedListingOrThrow(listingId);

    List<UnavailabilityInterval> intervals = findBusyIntervals(listing.getId(), from, to);

    List<UnavailabilityInterval> normalizedIntervals = normalizeIntervals(intervals);

    return availabilityMapper.toResponse(listing.getId(), from, to, normalizedIntervals);
}


---

## 13.3. Не использовать magic numbers и magic strings

Плохо:

if (Duration.between(from, to).toDays() > 93) {
    ...
}


Хорошо:

private static final int MAX_AVAILABILITY_RANGE_DAYS = 93;


Ещё лучше:

properties
roomhub.availability.max-range-days=93
roomhub.booking.slot-minutes=60


---

## 13.4. Не создавать premature abstraction

Не нужно заранее создавать:


AbstractListingProcessor
BaseAvailabilityHandler
GenericEntityMapper
CommonCrudService


Абстракция нужна только при реальном повторении и ясной пользе.

---

## 13.5. `common` не должен быть свалкой

В `common` можно класть:


ApiErrorResponse
GlobalExceptionHandler
PageResponse
BaseEntity
ClockConfig
SecurityUtils


Нельзя класть туда всё подряд:


ListingHelper
BookingUtils
UserManager
DataProcessor
CommonService


Если класс относится к listing — он должен быть в `listing`.
Если к booking — он должен быть в `booking`.
Если к availability — он должен быть в `availability`.

---

# 14. Logging

## 14.1. Логи должны отражать важные бизнес-события

Логировать стоит:


создание объявления;
публикацию объявления;
отправку на модерацию;
создание бронирования;
отмену бронирования;
ошибки платежей;
ошибки интеграций;
подозрительные действия;
ошибки авторизации.


---

## 14.2. Не засорять логи техническим шумом

Плохо:

javalog.info("Entered method getListings");
log.info("Mapping listing");
log.info("Returning response");


Хорошо:

log.info("Listing {} published by owner {}", listingId, ownerId);


---

# 15. Docker и production-readiness

## 15.1. Docker image не должен запускаться от root

Проверяй наличие non-root user.

---

## 15.2. Версия Java должна быть согласована

Если в `pom.xml` указана Java 17, Docker image тоже должен использовать Java 17, если нет осознанной причины для Java 21.

---

## 15.3. Не пропускать тесты без причины

Подозрительно:

dockerfile
RUN ./mvnw clean package -DskipTests


Если тесты пропускаются, должна быть понятная причина.

---

## 15.4. Docker build должен использовать cache эффективно

Хорошо:

dockerfile
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package


---

# 16. Приоритеты замечаний

## Critical / Request changes

Ставь `request changes`, если есть:


бизнес-логика в controller;
возврат Entity из public API;
отсутствие авторизации на mutating endpoint;
ошибки, позволяющие создать некорректные бронирования;
ошибки в расчёте availability;
отсутствие валидации критичных request fields;
опасные настройки production;
изменение старой Flyway-миграции;
использование double/float для денег;
SQL/data integrity риск;
утечка чувствительных данных.


---

## Major / Important

Отмечай как важное, если есть:


нет пагинации для списка;
строковые статусы вместо enum;
нет единого error response;
недостаточные DB constraints;
неясный timezone contract;
слабое покрытие тестами;
необновлённый OpenAPI;
слишком большой service method;
неудачная структура пакетов.


---

## Minor

Отмечай как minor:


неудачное название переменной;
избыточный boilerplate;
DTO можно заменить на record;
можно упростить stream;
можно улучшить сообщение ошибки;
можно улучшить JavaDoc/OpenAPI description.


---

# 17. Минимальный чеклист перед approve

Перед approve проверь:


[ ] Controller не содержит бизнес-логику.
[ ] Entity не возвращаются из API.
[ ] DTO специализированы под сценарии.
[ ] Request DTO валидируются.
[ ] Статусы и типы оформлены enum.
[ ] Списочные endpoints имеют пагинацию.
[ ] Ошибки возвращаются в едином формате.
[ ] Есть custom exceptions для бизнес-ошибок.
[ ] JPA entity соответствует Flyway schema.
[ ] БД защищает ключевые инварианты.
[ ] Availability учитывает edge cases.
[ ] Время и timezone обработаны явно.
[ ] Mutating endpoints защищены авторизацией.
[ ] OpenAPI обновлён.
[ ] Новая бизнес-логика покрыта тестами.
[ ] Новые endpoints покрыты controller tests.
[ ] Нет hardcoded production values.
[ ] Нет sensitive data в логах.
[ ] Миграции Flyway append-only.


---

# 18. Итоговый verdict

В конце ревью всегда указывай один из вариантов:

## Approve

Используй только если:


нет критичных проблем;
код соответствует архитектуре проекта;
основные сценарии покрыты тестами;
нет рисков для данных, безопасности и API-контракта.


## Approve with comments

Используй если:

есть minor/major замечания;
код можно merge-ить;
замечания можно исправить отдельной задачей.


## Request changes

Используй если:

- есть critical issues;
- код ломает контракт;
- есть риск повреждения данных;
- есть security-риск;
- нет тестов на критичную бизнес-логику;
- изменения ухудшают архитектуру.


---

# 19. Главный принцип ревью

Приоритет ревью:


1. Корректность бизнес-логики.
2. Безопасность.
3. Целостность данных.
4. Стабильность API-контракта.
5. Тестируемость.
6. Поддерживаемость.
7. Стиль кода.


Не трать основное внимание на стиль, если в коде есть архитектурные, security или data integrity проблемы.

Главный вопрос при ревью:

Сможет ли команда безопасно развивать RoomHubB2B дальше, если этот код попадёт в main?
