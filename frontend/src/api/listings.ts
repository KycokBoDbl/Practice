import { api } from './client'
import type { Listing } from '../types/listing'

export interface BusyInterval {
  startAt: string
  endAt: string
}

export interface ListingAvailability {
  listingId: number
  from: string
  to: string
  busyIntervals: BusyInterval[]
}

export async function getListings(): Promise<Listing[]> {
  const response = await api.get<Listing[]>('/api/listings')
  return response.data
}

export async function getListingAvailability(
  listingId: number,
  from: string,
  to: string,
): Promise<ListingAvailability> {
  const response = await api.get<ListingAvailability>(
    `/api/listings/${listingId}/availability`,
    {
      params: { from, to },
    },
  )

  return response.data
}
