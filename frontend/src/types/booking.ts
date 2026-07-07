export type BookingStatus =
  | 'REQUESTED'
  | 'AWAITING_CONFIRMATION'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'EXPIRED'

export type BookingTransitionCommand =
  | 'approve'
  | 'reject'
  | 'confirm'
  | 'cancel'

export type BookingInboxFilterStatus = BookingStatus

export interface BookingInboxFilters {
  status?: BookingInboxFilterStatus
}

export interface CreateBookingRequest {
  listingId: number
  startAt: string
  endAt: string
}

export interface BookingResponse {
  id: number
  listingId: number
  status: BookingStatus
  startAt: string
  endAt: string
  pricePerHour: number
  totalPrice: number
  confirmationDeadline: string | null
}

export interface BookingHistoryResponse {
  id: number
  fromStatus: BookingStatus | null
  toStatus: BookingStatus
  reason: string
  createdAt: string
}

export interface BookingInboxItem {
  id: number
  listingId: number
  listingTitle: string
  status: BookingStatus
  startAt: string
  endAt: string
  pricePerHour: number
  totalPrice: number
  confirmationDeadline: string | null
  tenantOrganizationName: string
  landlordOrganizationName: string
  createdAt: string
  updatedAt: string
}

export type BookingTransitionResponse = BookingResponse

export type BookingErrorKind =
  | 'validation'
  | 'unauthorized'
  | 'forbidden'
  | 'notFound'
  | 'conflict'
  | 'unknown'
