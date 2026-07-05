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

  useEffect(() => {
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

      try {
        const data = await getListingAvailability(
          listingId,
          `${toDateValue(monthStart)}T00:00`,
          `${toDateValue(nextMonthStart)}T00:00`,
        )

        setBusyIntervals(data.busyIntervals)
      } catch (error) {
        console.error('Failed to load listing availability:', error)
        setBusyIntervals([])
      } finally {
        setAvailabilityLoading(false)
      }
    }

    loadAvailability()
  }, [listingId, refreshKey, visibleMonth])

  return {
    availabilityLoading,
    busyIntervals,
  }
}
