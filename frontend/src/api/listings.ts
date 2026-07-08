import axios from 'axios'

import { api } from './client'
import { parseApiError, type ParsedProblemDetail } from './problemDetails'
import type {
  CreateListingRequest,
  Listing,
  ListingPublicationErrorKind,
} from '../types/listing'

export interface ParsedListingPublicationApiError extends ParsedProblemDetail {
  kind: ListingPublicationErrorKind
}

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

function getErrorStatus(error: unknown, parsedError: ParsedProblemDetail) {
  if (typeof parsedError.status === 'number') {
    return parsedError.status
  }

  if (axios.isAxiosError(error)) {
    return error.response?.status
  }

  return undefined
}

function getListingPublicationErrorKind(
  status: number | undefined,
): ListingPublicationErrorKind {
  if (status === 400) return 'validation'
  if (status === 401) return 'unauthorized'
  if (status === 403) return 'forbidden'
  return 'unknown'
}

export function parseListingPublicationApiError(
  error: unknown,
): ParsedListingPublicationApiError {
  const parsedError = parseApiError(error)
  const status = getErrorStatus(error, parsedError)

  return {
    ...parsedError,
    status,
    kind: getListingPublicationErrorKind(status),
  }
}

export async function getListings(): Promise<Listing[]> {
  const response = await api.get<Listing[]>('/api/listings')
  return response.data
}

export async function publishListing(
  request: CreateListingRequest,
): Promise<Listing> {
  const response = await api.post<Listing>('/api/listings', request)
  return response.data
}

export async function getListing(listingId: string | undefined): Promise<Listing | null> {
  const listings = await getListings()
  return listings.find((listing) => String(listing.id) === listingId) ?? null
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
