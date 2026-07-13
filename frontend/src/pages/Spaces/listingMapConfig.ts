export const YANDEX_MAPS_API_KEY_ENV = 'VITE_YANDEX_MAPS_API_KEY'

export const listingMapProvider = 'yandex-maps' as const

export function getYandexMapsApiKey() {
  return import.meta.env.VITE_YANDEX_MAPS_API_KEY?.trim() ?? ''
}

export function hasYandexMapsApiKey() {
  return getYandexMapsApiKey().length > 0
}
