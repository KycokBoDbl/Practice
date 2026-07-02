## Context

RoomHub B2B использует Spring Boot 4.1, Spring Web MVC и Maven Wrapper. Сейчас контракт `GET /api/listings` следует восстанавливать из controller и DTO вручную: runtime OpenAPI endpoint, экспортируемого файла и общего процесса обновления документации нет. Frontend-разработчику нужен как быстрый доступ к контракту запущенного backend, так и стабильный файл для code generation и review без необходимости держать backend запущенным.

Springdoc генерирует OpenAPI из Spring MVC mappings, Java-типов и Swagger-аннотаций в runtime. Для Spring Boot 4 используется совместимая ветка Springdoc 3.x; на момент проектирования актуальна версия 3.0.3.

## Goals / Non-Goals

**Goals:**

- Публиковать OpenAPI 3 JSON работающего приложения по `GET /api/openapi`.
- Ограничить документируемые операции публичным пространством `/api/**` и явно описать метаданные RoomHub B2B и listings response.
- Одной Maven-командой запускать backend на отдельном порту, получать runtime-документ и сохранять его в версионируемый `openapi/roomhub-b2b.openapi.json`.
- Сделать export fail-fast и воспроизводимым, чтобы diff файла отражал изменения API-контракта.
- Описать в корневом `README.md` запуск, runtime-доступ, export и регулярный процесс передачи контракта frontend.

**Non-Goals:**

- Генерация frontend-клиента или TypeScript-типов внутри backend-репозитория.
- Добавление Swagger UI: frontend нужен машиночитаемый контракт, а UI увеличивает runtime-зависимости без необходимости для MVP.
- Изменение URL, полей или поведения существующего `GET /api/listings`.
- Настройка конкретной CI-платформы; Maven-команда должна быть пригодна для последующего вызова из любой CI.
- Изменения PostgreSQL, Flyway или бизнес-логики listings.

## Decisions

### 1. Генерировать контракт через Springdoc в runtime

В `pom.xml` добавляется `org.springdoc:springdoc-openapi-starter-webmvc-api` версии 3.x, совместимой со Spring Boot 4.1. API-only starter отдаёт JSON/YAML без Swagger UI. Путь JSON настраивается как `/api/openapi`, а scan ограничивается `/api/**`.

Альтернатива: поддерживать статический YAML вручную. Она отклонена, потому что создаёт второй источник истины и не гарантирует соответствие Spring MVC mappings и DTO.

### 2. Хранить общие API-метаданные отдельно от бизнес-логики

Spring-managed конфигурация в пакете `config` задаёт название `RoomHub B2B API`, версию приложения и описание через OpenAPI annotations или bean. Операция listings и `ListingResponseDto` получают только те аннотации, которые нужны для устойчивого operation summary, response code и схемы полей; сериализация DTO не меняется.

Альтернатива: полагаться только на автоматический inference. Она короче, но даёт слабые описания и делает контракт менее понятным frontend-разработчику.

### 3. Экспортировать тот же runtime-документ Maven-профилем

Профиль `openapi-export` связывает `spring-boot-maven-plugin` start/stop и `springdoc-openapi-maven-plugin:generate` с integration-test lifecycle. Приложение запускается на выделенном настраиваемом порту, plugin запрашивает `/api/openapi`, включает `failOnError=true` и записывает JSON в `${project.basedir}/openapi/roomhub-b2b.openapi.json`. Основная команда для Windows: `mvnw.cmd verify -Popenapi-export`; эквивалент для Unix: `./mvnw verify -Popenapi-export`.

Export использует обычную конфигурацию datasource, поэтому PostgreSQL должен быть доступен так же, как при стандартном запуске backend. Это сохраняет реальный application context и не вводит отдельную тестовую базу или особый кодовый путь только для документации.

Альтернатива: `curl`/PowerShell-скрипт против вручную запущенного backend. Она остаётся допустимым способом локального сохранения runtime JSON, но не является основной командой, поскольку не управляет lifecycle приложения и сложнее воспроизводится в CI.

### 4. Версионировать generated contract и проверять его diff

Файл `openapi/roomhub-b2b.openapi.json` коммитится вместе с изменением API. Повторный export без изменения mappings, DTO и OpenAPI metadata не должен менять файл. README устанавливает порядок: изменить backend и аннотации, выполнить tests/export, проверить `git diff`, затем передать файл frontend или запустить его code generation.

Альтернатива: хранить export только в `target/`. Она отклонена, потому что frontend и code review не получают устойчивый артефакт контракта между запусками backend.

### 5. Проверять контракт на уровне MVC integration test

Тест запрашивает `/api/openapi`, проверяет успешный JSON response, OpenAPI version/metadata, наличие `GET /api/listings` и ссылку на схему элементов `ListingResponseDto`. Дополнительно экспорт-команда с `failOnError=true` подтверждает доступность runtime endpoint в полном application context.

## Risks / Trade-offs

- [Runtime-документ зависит от корректности annotations и Java-сигнатур] -> Проверять ключевой listings path и response schema автоматическим тестом, а generated file просматривать в diff.
- [Экспорт требует доступного PostgreSQL] -> Явно указать prerequisite и datasource environment variables в README; не скрывать проблему отдельным непроизводственным application context.
- [Выделенный порт экспорта может быть занят] -> Сделать порт Maven property с безопасным значением по умолчанию и описать override.
- [Generated JSON может содержать нестабильный порядок или environment-specific URL] -> Не добавлять динамические server URLs, фиксировать metadata и подтвердить повторным export отсутствие diff.
- [Публичный endpoint раскрывает структуру API] -> Документировать только `/api/**`; при появлении Spring Security отдельно принять решение об ограничении docs endpoint по окружениям.

## Migration Plan

1. Добавить Springdoc runtime dependency, конфигурацию пути и API metadata.
2. Аннотировать текущий listings endpoint и DTO без изменения сериализуемого контракта.
3. Добавить contract test и убедиться, что существующие тесты проходят.
4. Добавить Maven export profile, запустить его с доступным PostgreSQL и создать начальный JSON-файл.
5. Добавить README с командами и регулярным pipeline, затем проверить повторный export и diff.

Rollback выполняется удалением Springdoc dependency/configuration, export profile, contract test и generated-файла. База данных и бизнес endpoint-ы не требуют отката.

## Open Questions

Нет блокирующих вопросов. Ограничение runtime endpoint по окружению и подключение drift-check к конкретной CI-платформе следует решать отдельным изменением после появления security/CI требований.
