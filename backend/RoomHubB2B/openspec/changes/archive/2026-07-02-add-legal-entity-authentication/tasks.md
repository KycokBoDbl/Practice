## 1. Security dependencies and configuration

- [x] 1.1 Добавить Spring Security, OAuth2 Resource Server и JOSE зависимости в `pom.xml`; Definition of Done: Maven dependency resolution и компиляция проекта проходят без конфликтов Spring Boot 4.1.
- [x] 1.2 Добавить типизированные token properties для issuer, 15-минутного TTL и signing secret в `src/main/java/ru/esie/practice/roomhubb2b/config`; Definition of Done: context test подтверждает binding корректной конфигурации и отказ старта при секрете короче 256 бит.
- [x] 1.3 Обновить `src/main/resources/application.properties` безопасными placeholders/env bindings без сохраненного production secret; Definition of Done: секрет читается из окружения, а репозиторий не содержит рабочего ключа.

## 2. Database schema and persistence

- [x] 2.1 Создать Flyway migration `src/main/resources/db/migration/V6__create_legal_entity_accounts.sql` с `organizations`, `users`, FK, unique и role CHECK constraints; Definition of Done: миграция успешно применяется к PostgreSQL с существующими V1-V5 данными.
- [x] 2.2 Добавить `OrganizationEntity`, `UserEntity` и `UserRole` в `src/main/java/ru/esie/practice/roomhubb2b/auth`; Definition of Done: JPA mapping соответствует всем колонкам V6 и не допускает nullable organization, email, hash или role.
- [x] 2.3 Добавить `OrganizationRepository` и `UserRepository` с поиском по taxId, emailNormalized и id в `src/main/java/ru/esie/practice/roomhubb2b/auth`; Definition of Done: repository tests находят пользователя независимо от регистра введенного email после normalization.
- [x] 2.4 Добавить PostgreSQL integration tests схемы в `src/test/java/ru/esie/practice/roomhubb2b/auth/AuthSchemaTest.java`; Definition of Done: тесты доказывают уникальность taxId/emailNormalized, FK, role CHECK и невозможность некорректных строк.

## 3. Registration flow

- [x] 3.1 Добавить `RegisterRequestDto` и `ProfileResponseDto` в `src/main/java/ru/esie/practice/roomhubb2b/auth/dto` с validation для legalName, 10-значного taxId, email, role и password; Definition of Done: DTO tests покрывают пустые поля, 12-значный ИНН, неподдерживаемую роль и ограничения пароля в символах/UTF-8 байтах.
- [x] 3.2 Настроить `DelegatingPasswordEncoder` с bcrypt в `src/main/java/ru/esie/practice/roomhubb2b/config/SecurityConfig.java`; Definition of Done: unit test подтверждает проверку пароля, случайную соль и отсутствие сохранения plaintext.
- [x] 3.3 Реализовать locale-independent email normalization и transactional registration в `src/main/java/ru/esie/practice/roomhubb2b/auth/AuthService.java`; Definition of Done: успешная регистрация атомарно создает одну organization и одного user с ожидаемыми нормализованными данными.
- [x] 3.4 Обработать duplicate email/taxId, включая race по DB unique constraint, в auth service/exception handler; Definition of Done: service integration tests возвращают доменный conflict и не оставляют orphan organization.
- [x] 3.5 Добавить `POST /api/auth/register` в `src/main/java/ru/esie/practice/roomhubb2b/auth/AuthController.java`; Definition of Done: MockMvc tests подтверждают `201`, response без credentials, `400` для validation и `409` для дубликатов.

## 4. Login and access token

- [x] 4.1 Добавить `LoginRequestDto` и `TokenResponseDto` в `src/main/java/ru/esie/practice/roomhubb2b/auth/dto`; Definition of Done: serialization tests фиксируют `accessToken`, `tokenType=Bearer`, `expiresIn` и отсутствие внутренних полей.
- [x] 4.2 Реализовать JWT encoder/decoder и `AccessTokenService` в `src/main/java/ru/esie/practice/roomhubb2b/auth`; Definition of Done: unit tests проверяют HS256 signature, issuer, `sub`, `organizationId`, `role`, `iat`, `exp` и 15-минутный TTL с подменяемым `Clock`.
- [x] 4.3 Реализовать проверку нормализованного email/password и одинаковую ошибку неверных credentials в `AuthService`; Definition of Done: unit tests покрывают успешный вход, неверный пароль и неизвестный email без раскрытия существования пользователя.
- [x] 4.4 Добавить `POST /api/auth/login` в `AuthController`; Definition of Done: MockMvc tests подтверждают `200` с проверяемым token, `400` для malformed request и одинаковый `401` для обеих ошибок credentials.

## 5. Request authentication and profile

- [x] 5.1 Настроить stateless `SecurityFilterChain`, JWT authorities mapping и CSRF policy в `src/main/java/ru/esie/practice/roomhubb2b/config/SecurityConfig.java`; Definition of Done: сервер не создает HTTP session, а проверенный claim role преобразуется в ожидаемый authority.
- [x] 5.2 Реализовать публичный allowlist для auth, listings, availability и `/api/openapi`, закрыв остальные application routes по умолчанию; Definition of Done: security MockMvc tests подтверждают анонимный `200` для существующих GET endpoints и `401` для тестового защищенного endpoint.
- [x] 5.3 Реализовать `GET /api/auth/me` через загрузку актуальных user/organization по token subject в `AuthController` и `AuthService`; Definition of Done: MockMvc tests возвращают профиль владельца token и `401`, если user больше не существует.
- [x] 5.4 Добавить единый JSON error contract на основе `ProblemDetail` для validation, conflict, authentication и access denied в `src/main/java/ru/esie/practice/roomhubb2b/config` или `auth`; Definition of Done: controller/security tests фиксируют стабильные `400/401/403/409`, content type и отсутствие credentials в body.
- [x] 5.5 Добавить token integration tests в `src/test/java/ru/esie/practice/roomhubb2b/auth`; Definition of Done: валидный token допускается, а отсутствующий, просроченный, с неверным issuer и с измененной подписью получают `401`.

## 6. OpenAPI and documentation

- [x] 6.1 Аннотировать auth controller, DTO и error responses для springdoc и зарегистрировать bearer security scheme в `src/main/java/ru/esie/practice/roomhubb2b/config/OpenApiConfig.java`; Definition of Done: runtime `/api/openapi` содержит три auth operation, schemas и `400/401/403/409` без password hash.
- [x] 6.2 Расширить `src/test/java/ru/esie/practice/roomhubb2b/config/OpenApiContractTests.java`; Definition of Done: тесты подтверждают security requirement у `/api/auth/me`, отсутствие требования у публичных endpoints и неизменность listing/availability schemas.
- [x] 6.3 Обновить `openapi/roomhub-b2b.openapi.json` штатной командой `mvnw.cmd verify -Popenapi-export`; Definition of Done: generated diff содержит только ожидаемые auth paths, schemas, responses и bearer scheme.
- [x] 6.4 Обновить `README.md` примерами регистрации, входа, `/api/auth/me`, переменной signing secret и ограничениями MVP; Definition of Done: примеры request/response соответствуют экспортированному OpenAPI и не содержат реального секрета.

## 7. End-to-end verification

- [x] 7.1 Запустить полный `mvnw.cmd test` и устранить регрессии; Definition of Done: unit, MockMvc, JPA/Flyway и PostgreSQL integration tests проходят совместно.
- [x] 7.2 Запустить приложение с PostgreSQL и тестовым signing secret через `docker compose up --build`, затем выполнить smoke flow register -> login -> me; Definition of Done: зафиксированы `201`, `200`, `200`, профиль совпадает с регистрацией, а `/api/listings` остается доступен без token.
- [x] 7.3 Проверить негативный smoke flow duplicate registration, invalid credentials и expired/invalid token; Definition of Done: API возвращает соответственно `409`, `401`, `401` и не создает лишних строк organizations/users.
