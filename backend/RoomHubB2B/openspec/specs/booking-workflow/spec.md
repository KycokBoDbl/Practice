# Booking Workflow Specification

## Purpose

Определяет защищённый жизненный цикл бронирования коммерческих площадок между организациями арендатора и арендодателя, включая авторизацию участников, атомарное удержание календарных интервалов, историю состояний и автоматические временные переходы.
## Requirements
### Requirement: Создание заявки на бронирование

Система SHALL предоставлять `POST /api/bookings` аутентифицированному пользователю с ролью `TENANT` для создания заявки на существующее объявление со статусом `PUBLISHED` и назначенной организацией-владельцем. Организация арендатора MUST определяться из проверенного JWT и MUST NOT приниматься из request body. Запрашиваемый полуоткрытый интервал `[startAt, endAt)` MUST находиться в будущем, иметь `endAt` позже `startAt` и быть выровнен по целому часу.

#### Scenario: Корректная заявка создана
- **WHEN** аутентифицированный `TENANT` отправляет существующий `listingId` и будущий почасовой интервал
- **THEN** система отвечает `201 Created` и создаёт booking в состоянии `REQUESTED`
- **THEN** booking связан с organizationId из проверенного token, даже если клиент пытается передать идентификатор другой организации дополнительным полем
- **THEN** response содержит id, listingId, status, startAt, endAt, pricePerHour, totalPrice и пустой confirmationDeadline

#### Scenario: Объявление недоступно для бронирования
- **WHEN** listing отсутствует, имеет статус, отличный от `PUBLISHED`, либо не имеет назначенной организации-владельца
- **THEN** система отвечает `404 Not Found` и не создаёт booking

#### Scenario: Интервал заявки некорректен
- **WHEN** граница содержит минуты, секунды или доли секунды, `endAt <= startAt` либо `startAt <= now`
- **THEN** система отвечает `400 Bad Request` и не создаёт booking

#### Scenario: Неверная роль создаёт заявку
- **WHEN** аутентифицированный пользователь с ролью `LANDLORD` вызывает `POST /api/bookings`
- **THEN** система отвечает `403 Forbidden` и не создаёт booking

### Requirement: Авторизация участников сделки

Все booking endpoints MUST требовать валидный bearer token. Система MUST разрешать `approve` и `reject` только роли `LANDLORD` организации-владельца listing, а `confirm` и `cancel` — только роли `TENANT` организации, сохранённой в booking. `GET /api/bookings/{bookingId}` и `/history` MUST быть доступны только этим двум организациям.

#### Scenario: Владелец объявления обрабатывает заявку
- **WHEN** `LANDLORD`, чей organizationId совпадает с `listing.ownerOrganizationId`, вызывает approve или reject
- **THEN** система выполняет соответствующую команду по правилам текущего состояния

#### Scenario: Арендатор подтверждает или отменяет свою заявку
- **WHEN** `TENANT`, чей organizationId совпадает с `booking.tenantOrganizationId`, вызывает confirm или cancel
- **THEN** система выполняет соответствующую команду по правилам текущего состояния

#### Scenario: Операция вызвана без аутентификации
- **WHEN** booking endpoint вызывается без token либо с невалидным/просроченным token
- **THEN** система отвечает `401 Unauthorized` в существующем формате `ProblemDetail`

#### Scenario: Операция не соответствует роли
- **WHEN** `TENANT` вызывает approve/reject либо `LANDLORD` вызывает confirm/cancel
- **THEN** система отвечает `403 Forbidden` и не изменяет booking, календарь или историю

#### Scenario: Пользователь допустимой роли обращается к чужой сделке
- **WHEN** аутентифицированный пользователь имеет требуемую для операции роль, но его organizationId не является организацией соответствующего участника
- **THEN** система отвечает `404 Not Found` и не раскрывает существование или данные чужого booking

#### Scenario: Оба участника читают сделку
- **WHEN** booking или его историю запрашивает tenant organization сделки либо landlord organization исходного listing
- **THEN** система отвечает `200 OK`

### Requirement: Фиксация стоимости сделки

При создании booking система MUST скопировать текущую почасовую ставку объявления и MUST вычислить `totalPrice` как произведение ставки на целое число часов интервала.

#### Scenario: Стоимость рассчитана при создании
- **WHEN** ставка объявления равна `2500.00`, а интервал длится три часа
- **THEN** booking сохраняет `pricePerHour=2500.00` и `totalPrice=7500.00`

#### Scenario: Цена объявления изменена позднее
- **WHEN** после создания заявки цена исходного объявления изменяется
- **THEN** сохранённые pricePerHour и totalPrice booking остаются прежними

### Requirement: Разрешённые переходы конечного автомата

Система MUST изменять статус booking только через определённые команды или временные переходы: `REQUESTED -> AWAITING_CONFIRMATION`, `REQUESTED -> REJECTED`, `REQUESTED -> CANCELLED`, `AWAITING_CONFIRMATION -> CONFIRMED`, `AWAITING_CONFIRMATION -> EXPIRED`, `AWAITING_CONFIRMATION -> CANCELLED`, `CONFIRMED -> CANCELLED` до начала интервала, `CONFIRMED -> IN_PROGRESS` и `IN_PROGRESS -> COMPLETED`.

#### Scenario: Заявка одобрена
- **WHEN** команда `POST /api/bookings/{bookingId}/approve` применяется к booking в состоянии `REQUESTED` и интервал можно удержать
- **THEN** статус становится `AWAITING_CONFIRMATION` и response содержит confirmationDeadline

#### Scenario: Заявка отклонена
- **WHEN** команда reject применяется к booking в состоянии `REQUESTED`
- **THEN** статус становится `REJECTED`

#### Scenario: Одобренная заявка подтверждена вовремя
- **WHEN** команда confirm применяется к `AWAITING_CONFIRMATION` раньше confirmationDeadline
- **THEN** статус становится `CONFIRMED`, а календарный блок сохраняется

#### Scenario: Недопустимый переход отклонён
- **WHEN** команда не соответствует текущему состоянию booking
- **THEN** система отвечает `409 Conflict`, не меняет booking, календарь и историю

#### Scenario: Прямая запись статуса недоступна
- **WHEN** клиент пытается использовать универсальную CRUD-операцию изменения status
- **THEN** API не предоставляет такой операции

### Requirement: Атомарное удержание интервала

При успешном approve система MUST в одной транзакции повторно проверить занятость площадки, создать связанный календарный блок и изменить состояние booking. Для одной площадки никакие два успешно одобренных booking MUST NOT иметь непустого пересечения интервалов.

#### Scenario: Свободный интервал удержан
- **WHEN** booking в `REQUESTED` одобряется и его интервал не пересекает календарные блоки площадки
- **THEN** система создаёт один booking-блок и переводит booking в `AWAITING_CONFIRMATION`

#### Scenario: Две заявки конкурентно одобряются на одно время
- **WHEN** два параллельных approve обрабатывают пересекающиеся интервалы одной площадки
- **THEN** ровно один approve завершается успешно
- **THEN** второй отвечает `409 Conflict` и не создаёт календарный блок

#### Scenario: Интервал пересекает ручную недоступность
- **WHEN** интервал заявки пересекает существующий ручной блок `listing_unavailability_periods`
- **THEN** approve отвечает `409 Conflict` и booking остаётся `REQUESTED`

#### Scenario: Соседние интервалы не пересекаются
- **WHEN** `endAt` существующего блока равен `startAt` заявки либо `startAt` существующего блока равен `endAt` заявки
- **THEN** approve может успешно удержать интервал

#### Scenario: Начало аренды наступило до одобрения
- **WHEN** startAt booking не позже текущего времени при обработке approve
- **THEN** система отвечает `409 Conflict` и не создаёт календарный блок

### Requirement: Ограниченное по времени подтверждение

После approve система MUST установить `confirmationDeadline` равным более раннему из `approvedAt + 30 минут` и `startAt`. Неподтверждённый booking MUST перейти в `EXPIRED` при достижении deadline, а его календарный блок MUST быть удалён.

#### Scenario: Срок подтверждения истёк
- **WHEN** booking находится в `AWAITING_CONFIRMATION` и confirmationDeadline не позже now
- **THEN** система переводит booking в `EXPIRED` и освобождает его интервал

#### Scenario: Подтверждение пришло после срока
- **WHEN** confirm вызывается после confirmationDeadline до обработки booking планировщиком
- **THEN** система сначала переводит booking в `EXPIRED`, освобождает интервал и отвечает `409 Conflict` на confirm

#### Scenario: Освобождённый интервал можно удержать повторно
- **WHEN** booking истёк и другой `REQUESTED` booking на тот же интервал одобряется
- **THEN** второй booking может перейти в `AWAITING_CONFIRMATION`

### Requirement: Отмена сделки и освобождение резерва

Система SHALL разрешать cancel для `REQUESTED`, `AWAITING_CONFIRMATION` и `CONFIRMED` только до `startAt`. Отмена состояния с календарным блоком MUST удалить этот блок. `IN_PROGRESS`, `COMPLETED`, `REJECTED` и `EXPIRED` являются недоступными для новой отмены.

#### Scenario: Неподтверждённое удержание отменено
- **WHEN** cancel применяется к `AWAITING_CONFIRMATION` до startAt
- **THEN** статус становится `CANCELLED`, а booking-блок удаляется

#### Scenario: Подтверждённая будущая сделка отменена
- **WHEN** cancel применяется к `CONFIRMED` до startAt
- **THEN** статус становится `CANCELLED`, а интервал освобождается

#### Scenario: Начавшуюся сделку отменить нельзя
- **WHEN** cancel применяется после наступления startAt
- **THEN** система синхронизирует временной статус и отвечает `409 Conflict`

### Requirement: Автоматический жизненный цикл подтверждённой сделки

Система MUST переводить `CONFIRMED` booking в `IN_PROGRESS` при `startAt <= now` и `IN_PROGRESS` booking в `COMPLETED` при `endAt <= now`. Временные переходы MUST выполняться планировщиком и синхронизироваться при чтении либо команде над конкретным booking.

#### Scenario: Аренда началась
- **WHEN** подтверждённый booking достигает startAt
- **THEN** его статус становится `IN_PROGRESS`, а календарный блок сохраняется

#### Scenario: Аренда завершилась
- **WHEN** booking в `IN_PROGRESS` достигает endAt
- **THEN** его статус становится `COMPLETED`, а календарный блок сохраняется как прошедшая занятость

#### Scenario: Планировщик пропустил обе временные границы
- **WHEN** `CONFIRMED` booking впервые обрабатывается после endAt
- **THEN** система последовательно фиксирует переходы в `IN_PROGRESS` и `COMPLETED`

### Requirement: История состояний

Система MUST сохранять append-only запись каждого успешного создания или перехода с fromStatus, toStatus, причиной и временем. `GET /api/bookings/{bookingId}/history` SHALL возвращать историю в стабильном хронологическом порядке.

#### Scenario: Создание записано в историю
- **WHEN** booking успешно создан
- **THEN** история содержит одну запись с пустым fromStatus и `toStatus=REQUESTED`

#### Scenario: Переход записан один раз
- **WHEN** команда успешно меняет состояние booking
- **THEN** история получает ровно одну соответствующую запись перехода

#### Scenario: История получена
- **WHEN** клиент запрашивает историю существующего booking
- **THEN** система отвечает `200 OK` и сортирует записи по createdAt, затем по id

### Requirement: Идемпотентность команд перехода

Повтор команды, которая уже успешно привела booking в свой целевой статус, MUST возвращать текущее представление booking без повторного перехода, второго календарного блока или дублирующей записи истории.

#### Scenario: Approve повторён после успешного ответа
- **WHEN** approve повторно применяется к тому же booking в `AWAITING_CONFIRMATION`
- **THEN** система отвечает успешным текущим состоянием с первоначальным confirmationDeadline
- **THEN** количество календарных блоков и записей перехода не увеличивается

#### Scenario: Confirm повторён после успешного ответа
- **WHEN** confirm повторно применяется к тому же booking в `CONFIRMED` до startAt
- **THEN** система отвечает успешным текущим состоянием без новой записи истории

### Requirement: Интеграция с календарём доступности

Существующий `GET /api/listings/{listingId}/availability` SHALL включать booking-блоки состояний `AWAITING_CONFIRMATION`, `CONFIRMED`, `IN_PROGRESS` и `COMPLETED` наравне с ручными периодами и SHALL сохранять текущую JSON-схему ответа.

#### Scenario: Одобренный booking отображается занятым
- **WHEN** availability запрашивается для диапазона, пересекающего `AWAITING_CONFIRMATION` booking
- **THEN** busyIntervals содержит удержанный полуоткрытый интервал

#### Scenario: Отменённый или истёкший booking не занимает время
- **WHEN** booking перешёл в `CANCELLED` либо `EXPIRED`
- **THEN** его освобождённый интервал отсутствует в последующих ответах availability

#### Scenario: Контракт существующего endpoint сохранён
- **WHEN** frontend запрашивает availability после добавления booking workflow
- **THEN** response по-прежнему содержит listingId, from, to и busyIntervals с startAt/endAt

#### Scenario: Контракт публичного каталога сохранён
- **WHEN** frontend запрашивает `GET /api/listings` после добавления ownership
- **THEN** каждый объект по-прежнему соответствует `ListingResponseDto` с полями id, title, city, pricePerHour, capacity, spaceType, imageUrl, description и address
- **THEN** внутренний ownerOrganizationId отсутствует в response

### Requirement: Чтение сделки и обработка ошибок

Система SHALL предоставлять `GET /api/bookings/{bookingId}` и SHALL возвращать `404 Not Found` для отсутствующего или недоступного текущей организации booking. Ошибки формата и валидации SHALL возвращать `400 Bad Request`, отсутствие аутентификации SHALL возвращать `401 Unauthorized`, неверная роль SHALL возвращать `403 Forbidden`, конфликт состояния или календаря SHALL возвращать `409 Conflict`.

#### Scenario: Текущее состояние прочитано
- **WHEN** клиент запрашивает существующий booking
- **THEN** система синхронизирует его временные переходы и отвечает `200 OK` с актуальным состоянием

#### Scenario: Сделка отсутствует
- **WHEN** read или command endpoint получает несуществующий bookingId
- **THEN** система отвечает `404 Not Found`

### Requirement: Discovery of participant bookings
The booking workflow SHALL allow each booking participant organization to discover its own bookings through the booking inbox capability without requiring clients to know booking ids in advance.

#### Scenario: Participant discovers existing booking
- **WHEN** a booking exists for a tenant organization and a landlord organization
- **THEN** the tenant organization can discover it in its outgoing booking inbox
- **THEN** the landlord organization can discover it in its incoming booking inbox

#### Scenario: Non-participant cannot discover booking
- **WHEN** an authenticated organization is neither the booking tenant nor the owner organization of the booking listing
- **THEN** the booking is absent from that organization's inbox results

#### Scenario: Lifecycle behavior is unchanged by discovery
- **WHEN** a participant reads booking inbox results
- **THEN** existing create, detail, history, approve, reject, confirm, cancel, scheduler, conflict, and authorization behavior remains unchanged

### Requirement: Duplicate booking applications are rejected
The system SHALL allow an authenticated `TENANT` organization to have at most one booking application for the same published listing and the same local `startAt` calendar date. A duplicate `POST /api/bookings` attempt MUST fail with `409 Conflict` using the existing `application/problem+json` error format and MUST NOT create a booking or booking status history row.

#### Scenario: Tenant repeats an application for the same listing and day
- **WHEN** an authenticated `TENANT` already has a booking whose `listingId` matches the request and whose `startAt` is on the same local date as the new request
- **THEN** `POST /api/bookings` responds with `409 Conflict`
- **THEN** the response uses `application/problem+json` and explains that the tenant already has a booking request for this listing on this date
- **THEN** no additional booking or booking status history row is created

#### Scenario: Same tenant applies to the same listing on another day
- **WHEN** an authenticated `TENANT` already has a booking for a listing on one local `startAt` date
- **THEN** the same tenant can create a booking application for the same listing when the new `startAt` is on a different local date and all other booking creation rules pass

#### Scenario: Different tenant applies to the same listing and day
- **WHEN** one tenant organization already has a booking for a listing on a local `startAt` date
- **THEN** another tenant organization can create a booking application for the same listing and local `startAt` date when all other booking creation rules pass

#### Scenario: Concurrent duplicate submissions are serialized
- **WHEN** two concurrent `POST /api/bookings` requests from the same tenant target the same listing and local `startAt` date
- **THEN** at most one booking is committed
- **THEN** the losing request responds with `409 Conflict` instead of creating a duplicate row
