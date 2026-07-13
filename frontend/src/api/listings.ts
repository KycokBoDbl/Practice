import axios from 'axios'

import { api } from './client'
import { parseApiError, type ParsedProblemDetail } from './problemDetails'
import type {
  CreateListingRequest,
  Listing,
  ListingManagementErrorKind,
  ListingPublicationErrorKind,
  OwnedListing,
  UpdateListingRequest,
} from '../types/listing'

export interface ParsedListingPublicationApiError extends ParsedProblemDetail {
  kind: ListingPublicationErrorKind
}

export interface ParsedListingManagementApiError extends ParsedProblemDetail {
  kind: ListingManagementErrorKind
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

function getListingManagementErrorKind(
  status: number | undefined,
): ListingManagementErrorKind {
  if (status === 400) return 'validation'
  if (status === 401) return 'unauthorized'
  if (status === 403) return 'forbidden'
  if (status === 404) return 'notFound'
  if (status === 409) return 'conflict'
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

export function parseListingManagementApiError(
  error: unknown,
): ParsedListingManagementApiError {
  const parsedError = parseApiError(error)
  const status = getErrorStatus(error, parsedError)

  return {
    ...parsedError,
    status,
    kind: getListingManagementErrorKind(status),
  }
}

export async function getListings(): Promise<Listing[]> {
  const response = await api.get<Listing[]>('/api/listings')
  return response.data
}

export async function getOwnedListings(): Promise<OwnedListing[]> {
  const response = await api.get<OwnedListing[]>('/api/listings/owned')
  return response.data
}

export async function publishListing(
  request: CreateListingRequest,
): Promise<Listing> {
  const response = await api.post<Listing>('/api/listings', request)
  return response.data
}

export async function updateListing(
  listingId: number | string,
  request: UpdateListingRequest,
): Promise<Listing> {
  const response = await api.put<Listing>(`/api/listings/${listingId}`, request)
  return response.data
}

export async function hideListing(
  listingId: number | string,
): Promise<void> {
  await api.post(`/api/listings/${listingId}/hide`)
}

export async function activateListing(
  listingId: number | string,
): Promise<void> {
  await api.post(`/api/listings/${listingId}/activate`)
}

export async function deleteListing(
  listingId: number | string,
): Promise<void> {
  await api.delete(`/api/listings/${listingId}`)
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
