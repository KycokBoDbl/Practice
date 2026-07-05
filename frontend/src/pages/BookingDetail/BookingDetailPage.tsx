import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import {
  approveBooking,
  cancelBooking,
  getBooking,
  getBookingHistory,
  confirmBooking,
  parseBookingApiError,
  rejectBooking,
} from '../../api/bookings'
import { useAuth } from '../../auth/useAuth'
import type {
  BookingHistoryResponse,
  BookingResponse,
} from '../../types/booking'
import styles from './BookingDetailPage.module.css'

type BookingDetailState =
  | 'loading'
  | 'loaded'
  | 'notFound'
  | 'unavailable'
  | 'error'

type BookingAction = 'approve' | 'reject' | 'confirm' | 'cancel'

const DATE_TIME_FORMAT = new Intl.DateTimeFormat('ru-RU', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const BOOKING_STATUS_LABELS: Record<BookingResponse['status'], string> = {
  REQUESTED: 'Запрошена',
  AWAITING_CONFIRMATION: 'Ожидает подтверждения',
  CONFIRMED: 'Подтверждена',
  IN_PROGRESS: 'В процессе',
  COMPLETED: 'Завершена',
  REJECTED: 'Отклонена',
  CANCELLED: 'Отменена',
  EXPIRED: 'Истекла',
}

function formatDateTime(value: string | null) {
  if (!value) {
    return 'Не указано'
  }

  return DATE_TIME_FORMAT.format(new Date(value))
}

function formatStatus(status: BookingHistoryResponse['toStatus'] | null) {
  if (!status) {
    return 'Нет статуса'
  }

  return BOOKING_STATUS_LABELS[status]
}

function getBookingActionLabel(action: BookingAction) {
  if (action === 'approve') return 'Одобрить'
  if (action === 'reject') return 'Отклонить'
  if (action === 'confirm') return 'Подтвердить'
  return 'Отменить'
}

function isTerminalStatus(status: BookingResponse['status']) {
  return (
    status === 'COMPLETED' ||
    status === 'REJECTED' ||
    status === 'CANCELLED' ||
    status === 'EXPIRED'
  )
}

export function BookingDetailPage() {
  const { bookingId } = useParams()
  const { profile, loading: authLoading } = useAuth()
  const [booking, setBooking] = useState<BookingResponse | null>(null)
  const [history, setHistory] = useState<BookingHistoryResponse[]>([])
  const [state, setState] = useState<BookingDetailState>('loading')
  const [actionState, setActionState] = useState<{
    action: BookingAction | null
    message: string
    kind: 'info' | 'error'
  }>({
    action: null,
    message: '',
    kind: 'info',
  })

  useEffect(() => {
    let cancelled = false

    async function loadBooking() {
      setState('loading')
      setBooking(null)
      setHistory([])
      setActionState({ action: null, message: '', kind: 'info' })

      try {
        const [loadedBooking, loadedHistory] = await Promise.all([
          getBooking(bookingId ?? ''),
          getBookingHistory(bookingId ?? ''),
        ])

        if (cancelled) {
          return
        }

        setBooking(loadedBooking)
        setHistory(loadedHistory)
        setState('loaded')
      } catch (error) {
        if (cancelled) {
          return
        }

        const parsedError = parseBookingApiError(error)

        if (parsedError.kind === 'notFound') {
          setState('notFound')
          return
        }

        if (parsedError.kind === 'forbidden') {
          setState('unavailable')
          return
        }

        setState('error')
      }
    }

    loadBooking()

    return () => {
      cancelled = true
    }
  }, [bookingId])

  async function refreshBooking() {
    const targetBookingId = bookingId ?? ''
    const [loadedBooking, loadedHistory] = await Promise.all([
      getBooking(targetBookingId),
      getBookingHistory(targetBookingId),
    ])

    setBooking(loadedBooking)
    setHistory(loadedHistory)
    setState('loaded')
    return loadedBooking
  }

  async function handleAction(action: BookingAction) {
    if (!bookingId || !booking) {
      return
    }

    setActionState({
      action,
      message: '',
      kind: 'info',
    })

    try {
      if (action === 'approve') {
        setBooking(await approveBooking(bookingId))
      } else if (action === 'reject') {
        setBooking(await rejectBooking(bookingId))
      } else if (action === 'confirm') {
        setBooking(await confirmBooking(bookingId))
      } else {
        setBooking(await cancelBooking(bookingId))
      }

      const updatedBooking = await refreshBooking()
      setActionState({
        action: null,
        message: `Действие "${getBookingActionLabel(action)}" выполнено.`,
        kind: 'info',
      })
      setBooking(updatedBooking)
    } catch (error) {
      const parsedError = parseBookingApiError(error)
      const message =
        parsedError.kind === 'forbidden'
          ? 'Текущий аккаунт не может выполнить это действие.'
          : parsedError.kind === 'conflict'
            ? 'Бронь уже изменилась. Мы обновили данные и историю.'
            : parsedError.message

      setActionState({
        action: null,
        message,
        kind: 'error',
      })

      if (parsedError.kind === 'conflict') {
        try {
          await refreshBooking()
        } catch (refreshError) {
          console.error(refreshError)
        }
      }
    }
  }

  const canShowLandlordActions =
    !authLoading &&
    profile?.role === 'LANDLORD' &&
    booking &&
    booking.status === 'REQUESTED'

  const canShowTenantActions =
    !authLoading &&
    profile?.role === 'TENANT' &&
    booking &&
    booking.status === 'AWAITING_CONFIRMATION'

  const showActions = Boolean(
    booking &&
      !isTerminalStatus(booking.status) &&
      (canShowLandlordActions || canShowTenantActions),
  )

  if (state === 'loading') {
    return (
      <main className={styles.page}>
        <Link to="/" className={styles.backLink}>
          ← Вернуться к каталогу
        </Link>
        <div className={`${styles.panel} ${styles.stateBox}`}>Загрузка брони...</div>
      </main>
    )
  }

  if (state === 'notFound') {
    return (
      <main className={styles.page}>
        <Link to="/" className={styles.backLink}>
          ← Вернуться к каталогу
        </Link>
        <div className={`${styles.panel} ${styles.stateBox} ${styles.error}`}>
          <h1 className={styles.title}>Бронь не найдена</h1>
          <p className={styles.subtitle}>
            Запрошенная бронь недоступна или была удалена.
          </p>
        </div>
      </main>
    )
  }

  if (state === 'unavailable') {
    return (
      <main className={styles.page}>
        <Link to="/" className={styles.backLink}>
          ← Вернуться к каталогу
        </Link>
        <div className={`${styles.panel} ${styles.stateBox} ${styles.warning}`}>
          <h1 className={styles.title}>Бронь недоступна</h1>
          <p className={styles.subtitle}>
            У вас нет доступа к просмотру этой брони.
          </p>
        </div>
      </main>
    )
  }

  if (state === 'error' || !booking) {
    return (
      <main className={styles.page}>
        <Link to="/" className={styles.backLink}>
          ← Вернуться к каталогу
        </Link>
        <div className={`${styles.panel} ${styles.stateBox} ${styles.error}`}>
          <h1 className={styles.title}>Не удалось загрузить бронь</h1>
          <p className={styles.subtitle}>
            Попробуйте обновить страницу или вернуться позже.
          </p>
        </div>
      </main>
    )
  }

  return (
    <main className={styles.page}>
      <Link to={`/booking/${booking.listingId}`} className={styles.backLink}>
        ← Вернуться к броне помещения
      </Link>

      <section className={styles.panel}>
        <h1 className={styles.title}>Бронь #{booking.id}</h1>
        <p className={styles.subtitle}>
          Бронь по помещению {booking.listingId}
        </p>

        <dl className={styles.detailsGrid}>
          <div>
            <dt>Статус</dt>
            <dd>{BOOKING_STATUS_LABELS[booking.status]}</dd>
          </div>
          <div>
            <dt>Период</dt>
            <dd>
              {formatDateTime(booking.startAt)} - {formatDateTime(booking.endAt)}
            </dd>
          </div>
          <div>
            <dt>Цена за час</dt>
            <dd>{booking.pricePerHour.toLocaleString('ru-RU')} ₽</dd>
          </div>
          <div>
            <dt>Итог</dt>
            <dd>{booking.totalPrice.toLocaleString('ru-RU')} ₽</dd>
          </div>
          <div>
            <dt>Дедлайн подтверждения</dt>
            <dd>{formatDateTime(booking.confirmationDeadline)}</dd>
          </div>
        </dl>

        <h2 className={styles.sectionTitle}>История</h2>
        <ul className={styles.historyList}>
          {history.map((entry) => (
            <li key={entry.id} className={styles.historyItem}>
              <div className={styles.historyTop}>
                <span className={styles.historyLabel}>
                  {formatStatus(entry.fromStatus)} → {formatStatus(entry.toStatus)}
                </span>
                <span className={styles.historyValue}>
                  {formatDateTime(entry.createdAt)}
                </span>
              </div>
              <p className={styles.historyReason}>{entry.reason}</p>
            </li>
          ))}
        </ul>

        {showActions && booking && (
          <section className={styles.actionsSection}>
            <h2 className={styles.sectionTitle}>Действия</h2>
            <div className={styles.actionsGrid}>
              {canShowLandlordActions && (
                <>
                  <button
                    type="button"
                    className={styles.actionButton}
                    onClick={() => handleAction('approve')}
                    disabled={actionState.action !== null}
                  >
                    Одобрить
                  </button>
                  <button
                    type="button"
                    className={styles.actionButtonSecondary}
                    onClick={() => handleAction('reject')}
                    disabled={actionState.action !== null}
                  >
                    Отклонить
                  </button>
                </>
              )}

              {canShowTenantActions && (
                <>
                  <button
                    type="button"
                    className={styles.actionButton}
                    onClick={() => handleAction('confirm')}
                    disabled={actionState.action !== null}
                  >
                    Подтвердить
                  </button>
                  <button
                    type="button"
                    className={styles.actionButtonSecondary}
                    onClick={() => handleAction('cancel')}
                    disabled={actionState.action !== null}
                  >
                    Отменить
                  </button>
                </>
              )}
            </div>
            {actionState.message && (
              <p
                className={
                  actionState.kind === 'error' ? styles.actionError : styles.actionInfo
                }
                role={actionState.kind === 'error' ? 'alert' : 'status'}
              >
                {actionState.message}
              </p>
            )}
          </section>
        )}
      </section>
    </main>
  )
}
