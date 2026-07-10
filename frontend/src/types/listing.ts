import type { SpaceType } from './spaceType'

export type ListingLifecycleStatus = 'PUBLISHED' | 'ARCHIVED'

export interface Listing {
  id: number
  title: string
  description: string | null
  city: string
  address: string
  pricePerHour: number
  capacity: number
  spaceType: SpaceType
  imageUrl: string | null
  ownerOrganizationName: string | null
}

export interface OwnedListing extends Listing {
  status: ListingLifecycleStatus
}

export interface CreateListingRequest {
  title: string
  description: string | null
  city: string
  address: string
  pricePerHour: number
  capacity: number
  spaceType: SpaceType
  imageUrl: string | null
}

export type UpdateListingRequest = CreateListingRequest

export type ListingManagementErrorKind =
  | 'validation'
  | 'unauthorized'
  | 'forbidden'
  | 'notFound'
  | 'conflict'
  | 'unknown'

export type ListingPublicationErrorKind =
  | 'validation'
  | 'unauthorized'
  | 'forbidden'
  | 'unknown'
