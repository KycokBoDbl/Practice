# Frontend Review V3

## Summary

Текущий frontend в целом сохраняет понятное разделение на `api`, `auth`, `components`, `pages`, `router` и `types`, а последние декомпозиции заметно улучшили Booking Inbox и My Listings. При этом состояние после последнего коммита не готово к следующему этапу без исправлений: в нескольких user-visible файлах снова присутствует mojibake, а существующая автоматическая проверка его не ловит.

Риск сейчас высокий именно из-за качества пользовательского текста и слабой защиты от повторения этой ошибки. Архитектурно основные оставшиеся риски сосредоточены в каталоге/поиске, странице публикации, загрузке каталога и крупных route-компонентах.

## High Priority

### 1. User-visible mojibake снова попал в основные пользовательские сценарии

- `Location:` `src/pages/Spaces/SpacesPage.tsx:47`, `src/pages/Spaces/SpacesPage.tsx:104`, `src/pages/Spaces/SpacesPage.tsx:140`, `src/pages/Spaces/SpacesPage.tsx:189`
- `Location:` `src/pages/Spaces/CatalogSearch.tsx:184`, `src/pages/Spaces/CatalogSearch.tsx:199`, `src/pages/Spaces/CatalogSearch.tsx:241`
- `Location:` `src/pages/ListingPublication/ListingPublicationPage.tsx:172`, `src/pages/ListingPublication/ListingPublicationPage.tsx:212`, `src/pages/ListingPublication/ListingPublicationPage.tsx:439`
- `Location:` `src/pages/Spaces/SpacePage.tsx:85`, `src/pages/Spaces/SpacePage.tsx:121`, `src/pages/Spaces/SpacePage.tsx:180`
- `Location:` `src/pages/BookingDetail/BookingDetailPage.tsx:30`, `src/pages/BookingDetail/BookingDetailPage.tsx:177`
- `Issue:` Кириллица в заголовках, сообщениях ошибок, кнопках, фильтрах, карточках и деталях бронирования отображается как `Рџ...`, `СЃ...`, `в‚Ѕ`, `рџ...`.
- `Why it matters:` Это прямой пользовательский дефект. Он ломает каталог, публикацию, страницу объявления и детали бронирования даже при успешной сборке.
- `Direction:` Восстановить затронутые строки в UTF-8 и затем расширить автоматическую проверку так, чтобы она ловила текущие паттерны `Рџ`, `Рќ`, `Р’`, `СЃ`, `СЊ`, `в‚Ѕ`, `рџ`.

### 2. Mojibake-check дает false negative

- `Location:` `scripts/check-mojibake.mjs:6`
- `Location:` `src/pages/Spaces/SpacesPage.tsx:47`
- `Location:` `src/pages/Spaces/CatalogSearch.tsx:184`
- `Issue:` `npm run check:mojibake` проходит успешно, хотя в `src` есть явные mojibake-строки. Текущий список `PATTERNS` слишком узкий и не покрывает уже встречающиеся в проекте варианты повреждения текста.
- `Why it matters:` Команда создает ложное ощущение безопасности: дефект снова попал в коммит и не был остановлен автоматикой.
- `Direction:` Обновить паттерны и добавить минимальные fixture/самопроверку скрипта, чтобы проверка падала на реально встречающихся примерах из проекта.

### 3. Блокировка Enter на странице публикации ломает клавиатурную отправку через саму кнопку

- `Location:` `src/pages/ListingPublication/ListingPublicationPage.tsx:104`
- `Location:` `src/pages/ListingPublication/ListingPublicationPage.tsx:438`
- `Issue:` `handleFormKeyDown` отменяет любой `Enter` внутри формы, кроме `textarea`. Если фокус находится на кнопке публикации, клавиатурная активация Enter также всплывает до формы и отменяется.
- `Why it matters:` Требование "не публиковать по Enter в полях" выполнено слишком широко. Пользователь, который навигирует клавиатурой, может не суметь активировать основную кнопку через Enter.
- `Direction:` Не блокировать Enter, когда `event.target` является submit-кнопкой, либо перевести публикацию на явный `type="button"` с отдельным обработчиком клика и сохранить доступную клавиатурную активацию кнопки.

## Medium Priority

### 1. Ошибка загрузки каталога превращается в пустое состояние

- `Location:` `src/pages/Spaces/useListingsPolling.ts:25`
- `Location:` `src/pages/Spaces/SpacesPage.tsx:156`
- `Issue:` `useListingsPolling` только пишет ошибку в console и не возвращает `error` state. При initial failure `loading` сбрасывается, `listings` остается пустым, а страница показывает "помещений пока нет".
- `Why it matters:` Пользователь видит неверную причину проблемы. Это особенно заметно при недоступном backend или сетевой ошибке.
- `Direction:` Добавить в hook отдельный `error`/`message` state и отрисовывать recoverable error state в каталоге, не смешивая его с настоящим пустым каталогом.

### 2. Polling каталога допускает пересекающиеся запросы и stale overwrite

- `Location:` `src/pages/Spaces/useListingsPolling.ts:34`
- `Location:` `src/pages/Spaces/useListingsPolling.ts:35`
- `Issue:` `setInterval` запускает новый `getListings()` каждые 30 секунд без in-flight guard и generation check.
- `Why it matters:` При медленном backend старый ответ может прийти после нового и перезаписать более свежие данные.
- `Direction:` Использовать подход, похожий на `useBookingInbox`: in-flight guard, request generation или последовательный `setTimeout` после завершения запроса.

### 3. Страница объявления загружает весь каталог ради одной записи

- `Location:` `src/api/listings.ts:217`
- `Location:` `src/pages/Spaces/SpacePage.tsx:63`
- `Issue:` `getListing()` вызывает `getListings()` и ищет запись на клиенте.
- `Why it matters:` Детальная страница зависит от полного публичного каталога, получает лишние данные и наследует все проблемы общего списка. При росте каталога это будет дороже и менее надежно.
- `Direction:` Когда backend-контракт будет готов, заменить на точечный endpoint. Если backend менять нельзя, хотя бы явно зафиксировать это ограничение и не расширять логику detail page поверх полного списка.

### 4. Крупные route-компоненты все еще смешивают много ответственностей

- `Location:` `src/pages/MyListings/MyListingsPage.tsx:36`
- `Location:` `src/pages/ListingPublication/ListingPublicationPage.tsx:32`
- `Issue:` `MyListingsPage` остается около 681 строки, `ListingPublicationPage` около 445 строк. Они одновременно управляют формами, валидацией, scroll-to-error, модалками, API mutation state и крупной JSX-разметкой.
- `Why it matters:` Любая правка UI или поведения легко задевает соседние сценарии. Это уже проявлялось в визуальных регрессиях публикации и каталога.
- `Direction:` Продолжать выносить route-level orchestration в hooks, а модальные формы/preview/action panels — в небольшие presentational components.

### 5. После удаления UI тегов фильтров остался мертвый helper с битым текстом

- `Location:` `src/pages/Spaces/catalogFilters.ts:34`
- `Location:` `src/pages/Spaces/catalogFilters.ts:78`
- `Issue:` `CatalogFilterBadge` и `getActiveCatalogFilterBadges()` больше не используются в `CatalogSearch`, но остались экспортированными. Внутри helper также есть mojibake-строки.
- `Why it matters:` Мертвый код затрудняет ревью и поддерживает риск повторного использования уже сломанного текста.
- `Direction:` Удалить helper и связанные типы, если блок активных filter badges действительно исключен из UX.

## Low Priority

- `Location:` `src/api/listings.ts:44`  
  `Issue:` `looksLikeUtf8Mojibake()` выглядит как временная защита от backend seed-данных, но эвристика не покрывает текущие user-visible строки в frontend.  
  `Direction:` Не смешивать нормализацию backend demo data с проверкой исходников. Для исходников нужен отдельный строгий check.

- `Location:` `src/pages/Spaces/SpacesPage.tsx:112`  
  `Issue:` emoji-as-icons в production UI сейчас тоже повреждены mojibake (`рџ...`).  
  `Direction:` Заменить на нормальные текстовые/иконные компоненты или убрать, если баннер должен быть спокойнее.

- `Location:` `src/pages/BookingDetail/BookingDetailPage.tsx:152`  
  `Issue:` action handler, refresh logic и rendering живут в одном большом route-файле.  
  `Direction:` При следующей работе с booking detail вынести action state/refresh в hook.

## AI-Generated Code Signals

- This has AI-generated-code risk characteristics because user-visible text corruption appears across unrelated route files while build/lint still pass. Это похоже на механическую вставку/перекодирование без финальной UI-проверки.
- This has AI-generated-code risk characteristics because recent UI removal left exported dead helper code (`getActiveCatalogFilterBadges`) and broken labels in `catalogFilters.ts`.
- This has AI-generated-code risk characteristics because several route files remain very large and combine API, state, validation and JSX despite partial extraction.

## Verification

- `npm run check:mojibake` — passed, but result is not trustworthy because local evidence shows mojibake in multiple files.
- `npm run lint` — passed.
- `npm run build` — passed.
- No backend checks were run; review scope was `frontend/src`.

## Recommendations

1. Fix all user-visible mojibake in `src`, then update `scripts/check-mojibake.mjs` so it catches the exact patterns that currently slipped through.
2. Adjust publication form Enter handling so field-level Enter does not submit, but the actual publish button remains keyboard-accessible.
3. Add explicit catalog error state and protect listing polling from overlapping stale responses.
4. Remove dead filter badge helper code if active filter tags are no longer part of the UI.
5. Continue decomposing `MyListingsPage`, `ListingPublicationPage`, and `BookingDetailPage` only after correctness and text encoding are stable.

## OpenSpec Follow-Up

- `OpenSpec recommended:` yes
- `Reason:` Fixing mojibake, tightening the automated check, and adjusting catalog loading/polling touches shared UX and correctness across multiple frontend routes.
- `Suggested change title:` Stabilize frontend text integrity and catalog loading states
- `Suggested scope:` restore readable frontend strings, strengthen mojibake detection, preserve accessible publish-button behavior, add catalog loading/error separation, prevent stale listing polling responses, remove dead active-filter badge code.
