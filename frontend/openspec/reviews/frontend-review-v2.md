Summary

Фронтенд структурно разделен по базовым слоям (api, components, pages, router, types), но ключевые пользовательские сценарии сейчас
собраны не по устойчивым границам ответственности. Основной риск: каталог, поиск, детали помещения и бронирование загружают и       
интерпретируют данные разрозненно. Header стал бизнес-компонентом каталога, BookingCalendar содержит слишком много логики, а        
BookingPage имеет пользовательский баг состояния загрузки. Перед merge код требует доработки.

High Priority

1. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Booking/BookingPage.tsx:11               
   Issue: нет отдельного loading-состояния для загрузки помещения.                                                                  
   Why it matters: listing изначально null, поэтому страница сразу показывает “Помещение не найдено” до завершения запроса. Это     
   пользовательски видимый баг.                                                                                                     
   Direction: добавить явные состояния loading, loaded, notFound, error, как уже сделано в SpacePage.

2. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/Header/Header.tsx:28                
   Issue: Header загружает getListings() и управляет бизнес-фильтрами каталога.                                                     
   Why it matters: навигационный компонент теперь зависит от данных каталога на всех маршрутах, включая /login и /profile. Это      
   создает лишние запросы, связывает layout с каталогом и размывает поток данных.                                                   
   Direction: вынести поиск/фильтры в компонент каталога или отдельный CatalogSearch, а данные для городов получать на уровне       
   страницы/хука каталога.

3. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Spaces/SpacePage.tsx:18, /C:/Users/Илья/
   OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Booking/BookingPage.tsx:16                                        
   Issue: страницы деталей и бронирования загружают весь список помещений, чтобы найти одно по id.                                  
   Why it matters: это плохо масштабируется, дублирует логику и заставляет UI зависеть от полного каталога для detail route.        
   Direction: добавить API-функцию уровня getListing(id) или временно общий hook useListing(id), пока backend не даст отдельный     
   endpoint.

4. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/BookingCalendar/
   BookingCalendar.tsx:202                                                                                                          
   Issue: lint подтверждает React Hooks misuse: синхронные setState внутри effects на строках 212 и 218.                            
   Why it matters: это может вызывать лишние каскадные рендеры и показывает, что часть состояния является производной от занятости/
   выбранного слота.                                                                                                                
   Direction: пересмотреть модель состояния: вычислять допустимый слот/длительность как derived values или обновлять их только в    
   явных пользовательских действиях.

Medium Priority

1. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/BookingCalendar/                    
   BookingCalendar.tsx:134                                                                                                          
   Issue: компонент на 359 строк совмещает API-загрузку, date/time helpers, расчет занятости, выбор слота, расчет цены и rendering.
   Why it matters: любое изменение правил бронирования затронет большой компонент с высоким риском регрессий.                       
   Direction: разделить на useListingAvailability, чистые calendar utilities и небольшие view-компоненты.

2. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Spaces/SpacesPage.tsx:39                 
   Issue: страница каталога содержит загрузку, парсинг query params, фильтрацию, hero, empty states и карточки.                     
   Why it matters: фильтры уже частично дублируются с Header; правила поиска трудно тестировать отдельно.                           
   Direction: вынести фильтрацию в чистую функцию или hook, карточку помещения в отдельный компонент.

3. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/Header/Header.tsx:54                
   Issue: draft-состояния синхронизируются из URL params через effect.                                                              
   Why it matters: это уже падает на lint и создает две версии одного состояния: URL и локальные drafts.                            
   Direction: оставить URL как committed state, а draft state держать внутри отдельного search form с явной инициализацией при      
   открытии/submit/reset.

4. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/types/listing.ts:9, /C:/Users/Илья/OneDrive/   
   Рабочий стол/папка/Практика 3 курс/frontend/src/types/spaceType.ts:1                                                             
   Issue: spaceType типизирован как string, а labels как Record<string, string>.                                                    
   Why it matters: TypeScript не защищает от неизвестных типов помещений, хотя набор значений уже фактически фиксирован.            
   Direction: сделать SPACE_TYPE_LABELS typed constant и вывести union type для Listing.spaceType.

5. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/BookingCalendar/
   BookingCalendar.tsx:15                                                                                                           
   Issue: рабочие часы, длительности и шаг цены/времени захардкожены в компоненте.                                                  
   Why it matters: бизнес-правила бронирования будут меняться, а сейчас они размазаны по UI-логике.                                 
   Direction: вынести правила бронирования в конфиг/доменный модуль.

Low Priority

1. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Spaces/SpacePage.tsx:97                  
   Issue: вложены два одинаковых section className={styles.calendarSection}.                                                        
   Why it matters: лишняя DOM-обертка может ломать CSS-отступы и усложняет поддержку.                                               
   Direction: оставить один semantic container.

2. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Spaces/SpacesPage.tsx:104                
   Issue: внутри feature cards используются дополнительные <h1>.                                                                    
   Why it matters: нарушается heading hierarchy страницы.                                                                           
   Direction: заменить внутренние заголовки карточек на h2/h3.

3. Location: /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Spaces/SpacesPage.tsx:1                  
   Issue: стиль форматирования отличается от остального кода: двойные кавычки и semicolons рядом с файлами без них.                 
   Why it matters: это шумит в ревью и выглядит как несогласованная генерация/ручная правка.                                        
   Direction: привести форматирование через единый formatter/lint rule.

AI-Generated Code Signals

This has AI-generated-code risk characteristics because Header и SpacesPage независимо реализуют части одного сценария поиска/      
фильтрации: Header формирует URL params, а SpacesPage заново парсит и применяет их.

This has AI-generated-code risk characteristics because крупный BookingCalendar решает сразу несколько задач: API, календарная      
математика, бизнес-правила, выбор пользователя и JSX. Это типичный признак кода, собранного “в один компонент”, без последующего    
архитектурного прохода.

This has AI-generated-code risk characteristics because в SpacesPage есть смешение heading levels (h1 внутри feature cards), emoji-
as-icons и форматирование, отличающееся от соседних файлов.

This has AI-generated-code risk characteristics because есть визуально рабочие сценарии с пропущенными boundary states: BookingPage
не различает загрузку и “не найдено”, а ошибки API в основном уходят только в console.error.

Verification

npm run lint: failed.                                                                                                               
Ошибки:

- /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/BookingCalendar/BookingCalendar.tsx:212 react-
  hooks/set-state-in-effect

- /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/BookingCalendar/BookingCalendar.tsx:218 react-
  hooks/set-state-in-effect

- /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/Header/Header.tsx:55 react-hooks/set-state-in-
  effect

npm run build: did not complete because TypeScript could not write node_modules/.tmp/tsconfig.node.tsbuildinfo: EPERM: operation not
permitted. This is an environment/sandbox limitation, not confirmed source failure.

Recommendations

1. Исправить BookingPage: добавить loading/error/not found states.
2. Убрать загрузку каталога из Header; перенести поиск/фильтры в каталог или dedicated компонент.
3. Добавить getListing(id) или общий useListing(id) и убрать повторный getListings().find(...).
4. Разделить BookingCalendar: availability hook, pure date/time helpers, view-компоненты.
5. Вынести фильтрацию каталога из SpacesPage в тестируемую функцию/hook.
6. Усилить типы spaceType и вынести booking constants.
7. Почистить семантику и форматирование.

OpenSpec Follow-Up

OpenSpec recommended: yes

Reason: изменения затрагивают несколько маршрутов и общий поток данных: Header/search, catalog filtering, listing details, booking  
availability. Также может потребоваться новый API contract для getListing(id) и уточнение требований к фильтрам/бронированию.

Suggested change title: Refactor frontend catalog search and booking data flow

Suggested scope: search/filter ownership, listing detail loading, booking calendar state model, loading/error states, typed space   
types, and component boundary cleanup.