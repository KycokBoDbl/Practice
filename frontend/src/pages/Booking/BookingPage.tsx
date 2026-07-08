import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'

import {
  createBooking,
  parseBookingApiError,
} from '../../api/bookings'
import { getListing } from '../../api/listings'
import { useAuth } from '../../auth/useAuth'
import {
  BookingCalendar,
  type BookingCalendarPayload,
} from '../../components/BookingCalendar/BookingCalendar'
import type { BookingResponse } from '../../types/booking'
import type { Listing } from '../../types/listing'
import styles from './BookingPage.module.css'

type ListingLoadStatus = 'loading' | 'loaded' | 'notFound' | 'error'
type BookingSubmitStatus = 'idle' | 'submitting' | 'success' | 'error'

export function BookingPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { profile, loading: authLoading } = useAuth()
  const [listing, setListing] = useState<Listing | null>(null)
  const [status, setStatus] = useState<ListingLoadStatus>('loading')
  const [bookingStatus, setBookingStatus] = useState<BookingSubmitStatus>('idle')
  const [booking, setBooking] = useState<BookingResponse | null>(null)
  const [bookingMessage, setBookingMessage] = useState('')
  const [availabilityRefreshKey, setAvailabilityRefreshKey] = useState(0)

  useEffect(() => {
    let cancelled = false

    async function loadListing() {
      setStatus('loading')
      setListing(null)

      try {
        const foundListing = await getListing(id)

        if (cancelled) {
          return
        }

        setListing(foundListing ?? null)
        setStatus(foundListing ? 'loaded' : 'notFound')
      } catch (error) {
        console.error(error)

        if (!cancelled) {
          setListing(null)
          setStatus('error')
        }
      }
    }

    loadListing()

    return () => {
      cancelled = true
    }
  }, [id])

  async function handleConfirmBooking(payload: BookingCalendarPayload) {
    setBooking(null)
    setBookingMessage('')

    if (authLoading) {
      setBookingStatus('error')
      setBookingMessage('Проверяем состояние входа. Повторите действие через несколько секунд.')
      return
    }

    if (!profile) {
      navigate('/login', {
        state: {
          returnTo: `${location.pathname}${location.search}${location.hash}`,
        },
      })
      return
    }

    if (profile.role === 'LANDLORD') {
      setBookingStatus('error')
      setBookingMessage('Аккаунт арендодателя не может создавать заявки на бронирование.')
      return
    }

    setBookingStatus('submitting')

    try {
      const createdBooking = await createBooking({
        listingId: payload.listingId,
        startAt: payload.startAt,
        endAt: payload.endAt,
      })

      setBooking(createdBooking)
      setBookingStatus('success')
      setBookingMessage('')
    } catch (error) {
      const parsedError = parseBookingApiError(error)

      setBookingStatus('error')

      if (parsedError.kind === 'validation') {
        setBookingMessage(parsedError.message)
        return
      }

      if (parsedError.kind === 'unauthorized') {
        setBookingMessage('Сессия истекла. Войдите в аккаунт арендатора и повторите бронирование.')
        return
      }

      if (parsedError.kind === 'forbidden') {
        setBookingMessage('Текущий аккаунт не может выполнить это действие.')
        return
      }

      if (parsedError.kind === 'notFound') {
        setBookingMessage('Помещение или заявка недоступны. Проверьте страницу помещения и попробуйте снова.')
        return
      }

      if (parsedError.kind === 'conflict') {
        setAvailabilityRefreshKey((currentKey) => currentKey + 1)
        setBookingMessage('Выбранный слот уже недоступен. Мы обновили календарь, выберите другое время.')
        return
      }

      setBookingMessage(parsedError.message)
    }
  }

  if (status === 'loading') {
    return (
      <main className={styles.page}>
        <h1>Загрузка помещения...</h1>
      </main>
    )
  }

  if (status === 'error') {
    return (
      <main className={styles.page}>
        <h1>Не удалось загрузить помещение</h1>
      </main>
    )
  }

  if (status === 'notFound' || !listing) {
    return (
      <main className={styles.page}>
        <h1>Помещение не найдено</h1>
      </main>
    )
  }

  return (
    <main className={styles.page}>
      <Link
        to={`/spaces/${listing.id}`}
        className={styles.backLink}
      >
        ← Вернуться к помещению
      </Link>

      <section className={styles.card}>
        <h1 className={styles.title}>Бронирование помещения</h1>

        <p className={styles.subtitle}>
          <strong>{listing.title}</strong><br />
          {listing.city}, {listing.address}
        </p>

        <BookingCalendar
          listingId={listing.id}
          pricePerHour={listing.pricePerHour}
          bookingSubmitting={bookingStatus === 'submitting'}
          availabilityRefreshKey={availabilityRefreshKey}
          onConfirmBooking={handleConfirmBooking}
        />

        {bookingStatus === 'success' && booking && (
          <section className={styles.bookingResult} role="status">
            <h2>Заявка создана</h2>
            <p>
              <Link to={`/bookings/${booking.id}`} state={{ from: `/booking/${listing.id}` }}>
                Открыть детали брони
              </Link>
            </p>
            <dl className={styles.bookingDetails}>
              <div>
                <dt>Номер заявки</dt>
                <dd>{booking.id}</dd>
              </div>
              <div>
                <dt>Статус</dt>
                <dd>{booking.status}</dd>
              </div>
              <div>
                <dt>Период</dt>
                <dd>
                  {booking.startAt} - {booking.endAt}
                </dd>
              </div>
              <div>
                <dt>Стоимость</dt>
                <dd>{booking.totalPrice.toLocaleString('ru-RU')} ₽</dd>
              </div>
            </dl>
          </section>
        )}

        {bookingStatus === 'error' && bookingMessage && (
          <p className={styles.bookingError} role="alert">
            {bookingMessage}
          </p>
        )}
      </section>
    </main>
  )
}
