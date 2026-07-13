import { useEffect, useState } from 'react'

import { getListings } from '../../api/listings'
import type { Listing } from '../../types/listing'

const LISTINGS_POLL_INTERVAL_MS = 30_000

export function useListingsPolling() {
  const [listings, setListings] = useState<Listing[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    async function loadListings(isInitialLoad = false) {
      if (isInitialLoad) {
        setLoading(true)
      }

      try {
        const data = await getListings()

        if (!cancelled) {
          setListings(data)
        }
      } catch (error) {
        console.error('Ошибка при загрузке помещений:', error)
      } finally {
        if (!cancelled && isInitialLoad) {
          setLoading(false)
        }
      }
    }

    void loadListings(true)
    const pollTimer = window.setInterval(() => {
      void loadListings()
    }, LISTINGS_POLL_INTERVAL_MS)

    return () => {
      cancelled = true

      window.clearInterval(pollTimer)
    }
  }, [])

  return {
    listings,
    loading,
  }
}
