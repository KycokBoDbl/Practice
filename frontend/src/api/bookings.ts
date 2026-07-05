import axios from 'axios'

import { api } from './client'
import { parseApiError, type ParsedProblemDetail } from './problemDetails'
import type {
  BookingErrorKind,
  BookingHistoryResponse,
  BookingResponse,
  BookingTransitionCommand,
  BookingTransitionResponse,
  CreateBookingRequest,
} from '../types/booking'

export interface ParsedBookingApiError extends ParsedProblemDetail {
  kind: BookingErrorKind
}

const bookingTransitionPaths: Record<BookingTransitionCommand, string> = {
  approve: 'approve',
  reject: 'reject',
  confirm: 'confirm',
  cancel: 'cancel',
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

function getBookingErrorKind(status: number | undefined): BookingErrorKind {
  if (status === 400) return 'validation'
  if (status === 401) return 'unauthorized'
  if (status === 403) return 'forbidden'
  if (status === 404) return 'notFound'
  if (status === 409) return 'conflict'
  return 'unknown'
}

export function parseBookingApiError(error: unknown): ParsedBookingApiError {
  const parsedError = parseApiError(error)
  const status = getErrorStatus(error, parsedError)

  return {
    ...parsedError,
    status,
    kind: getBookingErrorKind(status),
  }
}

export async function createBooking(
  request: CreateBookingRequest,
): Promise<BookingResponse> {
  const response = await api.post<BookingResponse>('/api/bookings', request)
  return response.data
}

export async function getBooking(
  bookingId: number | string,
): Promise<BookingResponse> {
  const response = await api.get<BookingResponse>(`/api/bookings/${bookingId}`)
  return response.data
}

export async function getBookingHistory(
  bookingId: number | string,
): Promise<BookingHistoryResponse[]> {
  const response = await api.get<BookingHistoryResponse[]>(
    `/api/bookings/${bookingId}/history`,
  )
  return response.data
}

export async function transitionBooking(
  bookingId: number | string,
  command: BookingTransitionCommand,
): Promise<BookingTransitionResponse> {
  const response = await api.post<BookingTransitionResponse>(
    `/api/bookings/${bookingId}/${bookingTransitionPaths[command]}`,
  )
  return response.data
}

export function approveBooking(
  bookingId: number | string,
): Promise<BookingTransitionResponse> {
  return transitionBooking(bookingId, 'approve')
}

export function rejectBooking(
  bookingId: number | string,
): Promise<BookingTransitionResponse> {
  return transitionBooking(bookingId, 'reject')
}

export function confirmBooking(
  bookingId: number | string,
): Promise<BookingTransitionResponse> {
  return transitionBooking(bookingId, 'confirm')
}

export function cancelBooking(
  bookingId: number | string,
): Promise<BookingTransitionResponse> {
  return transitionBooking(bookingId, 'cancel')
}
