## Why

RoomHub должен различать арендаторов и арендодателей и связывать их действия с конкретным юридическим лицом. Сейчас backend не хранит учетные записи и не позволяет войти по email и паролю, поэтому невозможно безопасно ограничить будущие операции с объявлениями и бронированиями их участниками.

## What Changes

- Добавить регистрацию учетной записи юридического лица с одной выбранной ролью `LANDLORD` или `TENANT`.
- При регистрации обязательно принимать полное наименование организации, 10-значный ИНН, email и пароль. Проверять формат и уникальность реквизитов, но не обращаться во внешние реестры и не подтверждать фактическое существование организации.
- Добавить вход по зарегистрированным email и паролю с выдачей ограниченного по времени bearer access token.
- Добавить чтение профиля текущего пользователя по access token без возврата пароля или его хеша.
- Хранить пароль только в виде стойкого адаптивного хеша; одинаковые пароли не должны давать одинаковые сохраненные значения.
- Сохранять роль и связь учетной записи с организацией, чтобы последующие изменения могли авторизовать операции арендодателя и арендатора.
- Оставить `GET /api/listings`, `GET /api/listings/{listingId}/availability`, endpoints регистрации и входа публичными. Остальные endpoints по умолчанию требуют аутентификации после подключения Spring Security.
- Не включать в MVP проверку ФНС/ЕГРЮЛ, подтверждение email, восстановление пароля, refresh token, отзыв access token, приглашение сотрудников и несколько ролей у одной учетной записи.

Пример регистрации:

```http
POST /api/auth/register
Content-Type: application/json

{
  "role": "LANDLORD",
  "legalName": "ООО Деловой центр",
  "taxId": "2225123456",
  "email": "owner@example.com",
  "password": "S3cure-roomhub-password"
}
```

```json
{
  "userId": 12,
  "organizationId": 7,
  "role": "LANDLORD",
  "legalName": "ООО Деловой центр",
  "taxId": "2225123456",
  "email": "owner@example.com"
}
```

Пример входа:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "owner@example.com",
  "password": "S3cure-roomhub-password"
}
```

```json
{
  "accessToken": "<signed-token>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

## Capabilities

### New Capabilities

- `legal-entity-authentication`: Регистрация организаций-арендодателей и организаций-арендаторов, безопасная проверка email/пароля, выдача access token и получение профиля текущего пользователя.

### Modified Capabilities

Отсутствуют. Существующие публичные контракты каталога и календаря не меняются; детальная авторизация операций booking будет отдельным изменением после появления связи объявлений и сделок с организациями.

## Impact

- API: новые `POST /api/auth/register`, `POST /api/auth/login` и `GET /api/auth/me`; security scheme `bearerAuth` в OpenAPI. Существующие response schemas listings/availability не меняются.
- База данных: новые таблицы `organizations` и `users` с уникальными ИНН и нормализованным email, FK пользователя на организацию, CHECK-ограничением роли и колонкой password hash.
- Backend: новый модуль `auth`, Spring Security, password encoder, выпуск и проверка подписанных access tokens, security filter chain и единый JSON-контракт ошибок аутентификации/доступа.
- Конфигурация: секрет/ключ подписи и срок жизни access token задаются через свойства приложения и переменные окружения; секрет не хранится в репозитории.
- OpenAPI: runtime-аннотации и экспортируемый `openapi/roomhub-b2b.openapi.json` дополняются auth operations, schemas, ответами `400/401/409` и bearer security scheme.
- Frontend: появляются новые формы регистрации и входа, хранение access token и заголовок `Authorization: Bearer <token>` для закрытых запросов. Публичные экраны каталога продолжают работать без token.
- Зависимости: Spring Security и JOSE/JWT-компоненты Spring Security.

