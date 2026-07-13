import { useEffect, useState } from 'react'

import { getListingAvailability, type BusyInterval } from '../../api/listings'
import { toDateValue } from './utils'

export function useListingAvailability(
  listingId: number,
  visibleMonth: Date,
  refreshKey = 0,
) {
  const [busyIntervals, setBusyIntervals] = useState<BusyInterval[]>([])
  const [availabilityLoading, setAvailabilityLoading] = useState(false)
  const [availabilityError, setAvailabilityError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadAvailability() {
      const monthStart = new Date(
        visibleMonth.getFullYear(),
        visibleMonth.getMonth(),
        1,
      )
      const nextMonthStart = new Date(
        visibleMonth.getFullYear(),
        visibleMonth.getMonth() + 1,
        1,
      )

      setAvailabilityLoading(true)
      setAvailabilityError(null)

      try {
        const data = await getListingAvailability(
          listingId,
          `${toDateValue(monthStart)}T00:00`,
          `${toDateValue(nextMonthStart)}T00:00`,
        )

        if (cancelled) {
          return
        }

        setBusyIntervals(data.busyIntervals)
        setAvailabilityError(null)
      } catch (error) {
        console.error('Failed to load listing availability:', error)

        if (cancelled) {
          return
        }

        setAvailabilityError('Не удалось загрузить доступность. Попробуйте обновить календарь.')
      } finally {
        if (!cancelled) {
          setAvailabilityLoading(false)
        }
      }
    }

    loadAvailability()

    return () => {
      cancelled = true
    }
  }, [listingId, refreshKey, visibleMonth])

  return {
    availabilityError,
    availabilityLoading,
    busyIntervals,
  }
}
