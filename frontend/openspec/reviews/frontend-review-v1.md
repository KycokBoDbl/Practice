• Summary

The frontend has a reasonable top-level structure: API calls are in src/api, route screens are in src/pages, reusable UI is in src/
components, and shared models are in src/types. The main problems are not directory placement, but responsibility boundaries and    
duplicated data flow. Several components are doing too much, and catalog data is fetched independently in multiple places.

I did not modify any source files.

High Priority

1. /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/Header/Header.tsx:28 loads listings and owns  
   catalog filter logic.                                                                                                            
   Header is a navigation/layout component, but it fetches business data, derives cities, owns six draft filter states, mutates URL
   params, and controls filter UI. This couples every route to catalog data and causes an extra getListings() call even on pages    
   like login/profile. Move catalog filter data/loading closer to the catalog page or into a dedicated catalog search component/data
   hook.

2. /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Spaces/SpacePage.tsx:18 and /C:/Users/Илья/        
   OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Booking/BookingPage.tsx:16 fetch all listings to find one listing
   by id.                                                                                                                           
   This is inefficient and scales poorly. It also duplicates detail-loading logic. Add a getListing(id) API function if the backend
   supports it, or centralize the “load listings and find by id” behavior in a shared hook until the API exists.

3. /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Booking/BookingPage.tsx:27 has no loading state.   
   listing starts as null, so the page renders “Помещение не найдено” before the async request finishes. This is a user-visible bug.
   It should distinguish loading, not found, and loaded.

4. /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/BookingCalendar/BookingCalendar.tsx:160 is    
   overloaded.                                                                                                                      
   It fetches availability, computes calendar layout, parses time intervals, manages selected date/time/duration, renders the UI,   
   and contains booking summary behavior in one 359-line component. This makes booking behavior risky to extend. Split pure date/   
   status helpers, availability loading, and presentational calendar pieces.

Medium Priority

1. npm run lint currently fails.                                                                                                    
   Errors are in /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/Header/Header.tsx:55, /C:/Users/
   Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/BookingCalendar/BookingCalendar.tsx:212, and /C:/Users/
   Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/components/BookingCalendar/BookingCalendar.tsx:218. The rule       
   complains about synchronous setState inside effects. These states look partly derived from URL params or selected availability,  
   so this should be redesigned rather than suppressed.

2. /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Spaces/SpacesPage.tsx:39 mixes page rendering with
   filtering rules.                                                                                                                 
   The component owns data loading, query parsing, search tokenization, numeric range filtering, empty states, hero content, and    
   card rendering. Extracting a catalog query parser/filter function and a listing card component would make this easier to test and
   change.

3. /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/types/listing.ts:9 keeps spaceType as plain string,      
   while /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/types/spaceType.ts:1 uses Record<string, string>.  
   This loses type safety for known space types. A union type or enum-like as const map would catch invalid keys earlier.

4. /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Spaces/SpacePage.tsx:97 nests                      
   section.calendarSection inside another identical section.calendarSection.                                                        
   This is likely accidental duplicated markup and can create styling/layout surprises.

Low Priority

1. /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/pages/Spaces/SpacesPage.tsx:104 uses multiple feature-   
   card <h1> elements inside a page that already has a main <h1>. Use lower-level headings for correct document structure.

2. Formatting style is inconsistent: some files use semicolons and double quotes, others do not. This is minor, but it makes reviews
   noisier. Let ESLint/Prettier enforce one style.

3. /C:/Users/Илья/OneDrive/Рабочий стол/папка/Практика 3 курс/frontend/src/main.tsx:6 uses a non-null assertion for root. Acceptable
   in Vite apps, but a small runtime guard would be cleaner.

Recommendations

1. Fix the loading bug in BookingPage first.
2. Move listing detail loading into api/listings.ts as getListing(id) or into a shared hook.
3. Extract catalog filtering/query parsing from SpacesPage.
4. Move catalog search/filter UI out of Header or make it a dedicated component used only where needed.
5. Split BookingCalendar into availability hook, pure calendar utilities, and smaller view components.
6. Tighten spaceType typing with a typed constant map.

Verification: npm run lint fails with 3 React Hooks errors. npm run build could not complete because the sandbox blocked writing    
node_modules/.tmp/tsconfig.node.tsbuildinfo.                                                                                        
 
