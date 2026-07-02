## Why

Frontend-разработчику нужен актуальный машиночитаемый контракт backend API, иначе структура запросов и ответов передаётся вручную и легко расходится с кодом. Публикация OpenAPI из работающего приложения и воспроизводимый экспорт в файл сделают backend единственным источником контракта и позволят регулярно обновлять типы и API-клиент frontend.

## What Changes

- Добавить генерацию OpenAPI 3 для Spring Web MVC endpoint-ов и DTO RoomHub B2B.
- Отдавать актуальный JSON-контракт во время работы backend через документированный служебный endpoint.
- Добавить воспроизводимую Maven-команду экспорта того же runtime-контракта в версионируемый файл для передачи frontend-разработчику и проверки изменений через diff.
- Описать метаданные и схему существующего `GET /api/listings`, не меняя его URL, request или JSON response.
- Создать `README.md` с регулярным pipeline: изменить API и аннотации, запустить тесты, экспортировать контракт, проверить diff, передать или использовать файл во frontend и зафиксировать изменения вместе.
- Добавить автоматические проверки доступности OpenAPI и наличия в нём существующего listings endpoint.

Пример получения runtime-контракта:

```http
GET /api/openapi
Accept: application/json
```

Пример фрагмента ответа:

```json
{
  "openapi": "3.1.0",
  "info": {
    "title": "RoomHub B2B API"
  },
  "paths": {
    "/api/listings": {
      "get": {}
    }
  }
}
```

## Capabilities

### New Capabilities

- `openapi-contract-publishing`: Runtime-публикация и воспроизводимый файловый экспорт OpenAPI-контракта, а также процесс его регулярного обновления для frontend.

### Modified Capabilities

Нет.

## Impact

- Backend: конфигурация и API-аннотации для метаданных OpenAPI; бизнес-логика и публичный контракт `GET /api/listings` не меняются.
- API: добавляется служебный read-only endpoint `GET /api/openapi`; существующие endpoint-ы и их response не имеют breaking changes.
- Build: в `pom.xml` добавляются совместимые со Spring Boot 4.1 зависимости и Maven-настройки Springdoc для runtime и экспорта.
- Документация и артефакты: добавляются `README.md` и экспортируемый OpenAPI JSON-файл, пригодный для генерации frontend-типов или клиента.
- Тестирование: проверяется корректность OpenAPI-документа и присутствие в нём `/api/listings` с ожидаемой response schema.
- База данных: таблицы, поля и Flyway-миграции не затрагиваются.
