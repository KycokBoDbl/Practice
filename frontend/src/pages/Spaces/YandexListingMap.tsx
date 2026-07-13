import { useEffect, useId, useState } from 'react'

import type { Listing } from '../../types/listing'
import { getYandexMapsApiKey, YANDEX_MAPS_API_KEY_ENV } from './listingMapConfig'
import { getListingMapAddress, hasListingCoordinates } from './listingMapHelpers'
import styles from './SpacePage.module.css'

declare global {
  interface Window {
    ymaps?: YandexMapsGlobal
  }
}

interface YandexMapInstance {
  destroy: () => void
  geoObjects: {
    add: (geoObject: unknown) => void
  }
}

interface YandexMapsGlobal {
  ready: (callback: () => void) => void
  Map: new (
    element: HTMLElement,
    state: {
      center: [number, number]
      controls: string[]
      zoom: number
    },
    options?: Record<string, unknown>,
  ) => YandexMapInstance
  Placemark: new (
    coordinates: [number, number],
    properties?: Record<string, unknown>,
    options?: Record<string, unknown>,
  ) => unknown
}

interface YandexListingMapProps {
  title: string
  city: string
  address: string
  latitude: Listing['latitude']
  longitude: Listing['longitude']
  onClose: () => void
}

let yandexMapsLoader: Promise<YandexMapsGlobal> | null = null
const YANDEX_MAPS_SCRIPT_ID = 'yandex-maps-sdk'

function loadYandexMapsApi() {
  if (window.ymaps) {
    return Promise.resolve(window.ymaps)
  }

  if (yandexMapsLoader) {
    return yandexMapsLoader
  }

  const apiKey = getYandexMapsApiKey()

  if (!apiKey) {
    return Promise.reject(
      new Error(`Yandex Maps API key is missing. Set ${YANDEX_MAPS_API_KEY_ENV}.`),
    )
  }

  yandexMapsLoader = new Promise<YandexMapsGlobal>((resolve, reject) => {
    const existingScript = document.getElementById(YANDEX_MAPS_SCRIPT_ID)

    if (existingScript) {
      existingScript.remove()
    }

    const script = document.createElement('script')
    script.id = YANDEX_MAPS_SCRIPT_ID
    script.src = `https://api-maps.yandex.ru/2.1/?apikey=${encodeURIComponent(apiKey)}&lang=ru_RU`
    script.async = true

    script.onload = () => {
      if (!window.ymaps) {
        yandexMapsLoader = null
        reject(new Error('Yandex Maps SDK loaded without ymaps global.'))
        return
      }

      resolve(window.ymaps)
    }

    script.onerror = () => {
      yandexMapsLoader = null
      script.remove()
      reject(new Error('Failed to load Yandex Maps SDK.'))
    }

    document.head.appendChild(script)
  })

  return yandexMapsLoader
}

export function YandexListingMap({
  title,
  city,
  address,
  latitude,
  longitude,
  onClose,
}: YandexListingMapProps) {
  const mapId = useId().replace(/:/g, '-')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const mapAddress = getListingMapAddress({ address, city })
  const hasCoordinates = hasListingCoordinates({ latitude, longitude })
  const unavailableError = 'Координаты для этого адреса пока недоступны.'
  const resolvedError = hasCoordinates ? error : unavailableError
  const isLoading = hasCoordinates ? loading : false

  useEffect(() => {
    if (!hasCoordinates) {
      return
    }

    let disposed = false
    let map: YandexMapInstance | null = null

    async function mountMap() {
      try {
        const ymaps = await loadYandexMapsApi()

        if (disposed) {
          return
        }

        ymaps.ready(() => {
          if (disposed) {
            return
          }

          const container = document.getElementById(mapId)

          if (!container || latitude == null || longitude == null) {
            setError('Не удалось подготовить контейнер карты.')
            setLoading(false)
            return
          }

          const center: [number, number] = [latitude, longitude]

          map = new ymaps.Map(
            container,
            {
              center,
              controls: ['zoomControl', 'fullscreenControl'],
              zoom: 16,
            },
            {
              suppressMapOpenBlock: true,
            },
          )

          const placemark = new ymaps.Placemark(
            center,
            {
              balloonContentHeader: title,
              balloonContentBody: mapAddress,
              hintContent: title,
            },
            {
              preset: 'islands#blueDotIcon',
            },
          )

          map.geoObjects.add(placemark)
          setLoading(false)
        })
      } catch (nextError) {
        if (!disposed) {
          setError(
            nextError instanceof Error ? nextError.message : 'Не удалось загрузить карту.',
          )
          setLoading(false)
        }
      }
    }

    mountMap()

    return () => {
      disposed = true
      map?.destroy()
    }
  }, [hasCoordinates, latitude, longitude, mapAddress, mapId, title])

  return (
    <div
      className={styles.mapCard}
      role="dialog"
      aria-modal="false"
      aria-labelledby={`${mapId}-title`}
    >
      <div className={styles.mapCardHeader}>
        <div>
          <p id={`${mapId}-title`} className={styles.mapCardTitle}>
            {title}
          </p>
          <p className={styles.mapCardAddress}>{mapAddress}</p>
        </div>

        <button type="button" className={styles.mapCardClose} onClick={onClose}>
          Закрыть
        </button>
      </div>

      {isLoading && (
        <div className={styles.mapStatus} role="status">
          Загружаем карту...
        </div>
      )}

      {resolvedError ? (
        <div className={styles.mapError} role="status">
          <p className={styles.mapErrorTitle}>Карта сейчас недоступна</p>
          <p className={styles.mapErrorText}>{resolvedError}</p>
        </div>
      ) : (
        <div
          id={mapId}
          className={`${styles.mapCanvas} ${isLoading ? styles.mapCanvasHidden : ''}`}
        />
      )}
    </div>
  )
}
