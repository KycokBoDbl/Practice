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
```

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/roomhub_b2b
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
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

Проверка drift для CI или перед commit:

```powershell
.\mvnw.cmd verify -Popenapi-export
git diff --exit-code -- openapi/roomhub-b2b.openapi.json
```

```bash
./mvnw verify -Popenapi-export
git diff --exit-code -- openapi/roomhub-b2b.openapi.json
```
