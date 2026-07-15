import { useEffect, useRef, useState } from 'react'

import { getListings } from '../../api/listings'
import type { Listing } from '../../types/listing'

const LISTINGS_POLL_INTERVAL_MS = 30_000
const LISTINGS_LOAD_ERROR_MESSAGE = 'Не удалось загрузить каталог помещений.'

export function useListingsPolling() {
  const [listings, setListings] = useState<Listing[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const requestInFlightRef = useRef(false)

  useEffect(() => {
    let cancelled = false
    let pollTimer: number | null = null

    async function loadListings(isInitialLoad = false) {
      if (requestInFlightRef.current) {
        return
      }

      requestInFlightRef.current = true

      if (isInitialLoad) {
        setLoading(true)
      }

      try {
        const data = await getListings()

        if (!cancelled) {
          setListings(data)
          setErrorMessage('')
        }
      } catch (error) {
        console.error('Failed to load listings:', error)

        if (!cancelled) {
          setErrorMessage(LISTINGS_LOAD_ERROR_MESSAGE)
        }
      } finally {
        requestInFlightRef.current = false

        if (!cancelled && isInitialLoad) {
          setLoading(false)
        }
      }
    }

    function schedulePolling() {
      pollTimer = window.setTimeout(() => {
        void loadListings().finally(() => {
          if (!cancelled) {
            schedulePolling()
          }
        })
      }, LISTINGS_POLL_INTERVAL_MS)
    }

    void loadListings(true).finally(() => {
      if (!cancelled) {
        schedulePolling()
      }
    })

    return () => {
      cancelled = true

      if (pollTimer !== null) {
        window.clearTimeout(pollTimer)
      }
    }
  }, [])

  return {
    errorMessage,
    listings,
    loading,
  }
}
