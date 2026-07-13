# RoomHub B2B

## Быстрый запуск

Перед запуском должен быть установлен и запущен Docker Desktop.

Из корня проекта выполнить:

PowerShell:

```powershell
$env:ROOMHUB_AUTH_TOKEN_SECRET = "replace-with-at-least-32-random-bytes"
$env:ROOMHUB_GEOCODING_YANDEX_API_KEY = "<yandex-geocoder-key>"
docker compose up --build
```

Linux/macOS:

```bash
export ROOMHUB_AUTH_TOKEN_SECRET=replace-with-at-least-32-random-bytes
docker compose up --build
```

Для production используйте случайный signing secret из secret storage; значения выше являются placeholders.

После успешного запуска приложение будет доступно по адресу:

```
http://localhost:3000
```

## Остановка проекта

```bash
docker compose down
```

## Основной сценарий работы

Frontend запрашивает список помещений через REST API backend.

Backend получает данные из PostgreSQL и возвращает их на frontend для отображения в каталоге.

Просмотреть каталог помещений можно через пункт меню **«Помещения»** или перейти по адресу:

```
http://localhost:3000/spaces
```
