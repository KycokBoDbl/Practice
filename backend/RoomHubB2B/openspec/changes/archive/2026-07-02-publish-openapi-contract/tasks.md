## 1. Runtime OpenAPI

- [x] 1.1 Добавить совместимый со Spring Boot 4.1 `springdoc-openapi-starter-webmvc-api` 3.x и настроить путь `/api/openapi` со scan только `/api/**`. Файлы: `pom.xml`, `src/main/resources/application.properties`. Definition of Done: приложение собирается, а конфигурация Springdoc не меняет существующие `/api` mappings.
- [x] 1.2 Добавить общие metadata `RoomHub B2B API` и версию контракта в Spring-managed OpenAPI configuration. Файлы: `src/main/java/ru/esie/practice/roomhubb2b/config/OpenApiConfig.java`. Definition of Done: runtime JSON содержит ожидаемые `info.title` и `info.version` без environment-specific server URL.
- [x] 1.3 Описать `GET /api/listings`, успешный array response и поля `ListingResponseDto` минимальными OpenAPI-аннотациями. Файлы: `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingController.java`, `src/main/java/ru/esie/practice/roomhubb2b/listing/dto/ListingResponseDto.java`. Definition of Done: generated schema содержит все девять camelCase-полей, а фактический JSON listings не изменился.

## 2. Contract Verification

- [x] 2.1 Добавить MVC contract test для `GET /api/openapi`: HTTP 200, OpenAPI 3, metadata, `paths./api/listings.get` и array item schema `ListingResponseDto`. Файлы: `src/test/java/ru/esie/practice/roomhubb2b/config/OpenApiContractTests.java`. Definition of Done: тест падает при удалении listings path или обязательного поля схемы и проходит для актуального контракта.
- [x] 2.2 Запустить полный набор Maven-тестов и проверить регрессию существующего listings response. Файлы проверки: `src/test/java/ru/esie/practice/roomhubb2b/RoomHubB2BApplicationTests.java`, новый contract test; производственные файлы меняются только при найденном дефекте. Definition of Done: `mvnw.cmd test` завершается с кодом 0 при доступной тестовой инфраструктуре, а имена полей ответа остаются прежними.

## 3. Reproducible Export

- [x] 3.1 Добавить Maven-профиль `openapi-export`, который настраивает отдельный overridable port, запускает и останавливает Spring Boot, вызывает `springdoc-openapi-maven-plugin:generate`, пишет `openapi/roomhub-b2b.openapi.json` и использует `failOnError=true`. Файлы: `pom.xml`. Definition of Done: `mvnw.cmd verify -Popenapi-export` управляет lifecycle приложения и возвращает non-zero при недоступном runtime contract.
- [x] 3.2 С доступным PostgreSQL выполнить export и добавить начальный версионируемый контракт. Файлы: `openapi/roomhub-b2b.openapi.json`. Definition of Done: файл является валидным OpenAPI JSON, содержит `/api/listings`, а повторный export без изменений не создаёт diff.

## 4. Developer Pipeline Documentation

- [x] 4.1 Создать корневой README с prerequisites, datasource variables, локальным запуском backend, runtime URL `/api/openapi`, Windows/Unix export-командами, override export port и расположением generated-файла. Файлы: `README.md`. Definition of Done: новый разработчик может получить runtime или файловый контракт, следуя только README.
- [x] 4.2 Описать в README регулярный pipeline изменения API: обновить код и metadata, запустить тесты, экспортировать JSON, проверить `git diff`, закоммитить контракт вместе с backend и передать файл frontend для code generation. Файлы: `README.md`. Definition of Done: процесс отдельно объясняет contract-changing и internal-only изменения и содержит команды проверки drift.

## 5. End-to-End Check

- [x] 5.1 Выполнить `mvnw.cmd test`, `mvnw.cmd verify -Popenapi-export` и ручной GET runtime endpoint, затем сверить runtime и файловый список paths/schemas. Проверяемые файлы: `pom.xml`, `src/main/resources/application.properties`, `src/main/java/ru/esie/practice/roomhubb2b/config/OpenApiConfig.java`, `openapi/roomhub-b2b.openapi.json`, `README.md`. Definition of Done: все команды успешны, endpoint возвращает HTTP 200, экспорт не оставляет запущенный процесс и повторный export не меняет контракт.
