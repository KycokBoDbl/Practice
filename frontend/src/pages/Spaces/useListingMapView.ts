import { useCallback, useState } from 'react'

import type { Listing } from '../../types/listing'
import { hasListingCoordinates } from './listingMapHelpers'

export interface ListingMapSelection {
  listing: Listing
}

export function useListingMapView() {
  const [selection, setSelection] = useState<ListingMapSelection | null>(null)

  const openListingMap = useCallback((listing: Listing) => {
    setSelection({ listing })
  }, [])

  const closeListingMap = useCallback(() => {
    setSelection(null)
  }, [])

  return {
    closeListingMap,
    hasMapCoordinates: selection ? hasListingCoordinates(selection.listing) : false,
    isMapOpen: selection !== null,
    openListingMap,
    selectedListing: selection?.listing ?? null,
  }
}
