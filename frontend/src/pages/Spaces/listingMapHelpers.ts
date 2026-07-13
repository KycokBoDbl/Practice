import type { Listing } from '../../types/listing'

export function hasListingCoordinates(listing: Pick<Listing, 'latitude' | 'longitude'> | null | undefined) {
  return listing != null && listing.latitude != null && listing.longitude != null
}

export function getListingMapAddress(listing: Pick<Listing, 'city' | 'address'>) {
  return [listing.city.trim(), listing.address.trim()].filter(Boolean).join(', ')
}
