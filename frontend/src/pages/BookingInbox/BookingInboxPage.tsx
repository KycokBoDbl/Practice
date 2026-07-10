import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'

import { getBookingInbox, parseBookingApiError } from '../../api/bookings'
import { useAuth } from '../../auth/useAuth'
import type { BookingInboxItem } from '../../types/booking'
import styles from './BookingInboxPage.module.css'

type InboxState = 'loading' | 'loaded' | 'empty' | 'error'
type InboxView = 'all' | 'action-required' | 'confirmed' | 'active' | 'completed'

const POLL_INTERVAL_MS = 30000

const DATE_TIME_FORMAT = new Intl.DateTimeFormat('ru-RU', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const BOOKING_STATUS_LABELS: Record<BookingInboxItem['status'], string> = {
  REQUESTED: 'Запрошена',
  AWAITING_CONFIRMATION: 'Ожидает подтверждения',
  CONFIRMED: 'Подтверждена',
  IN_PROGRESS: 'В процессе',
  COMPLETED: 'Завершена',
  REJECTED: 'Отклонена',
  CANCELLED: 'Отменена',
  EXPIRED: 'Истекла',
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return 'Не указано'
  }

  return DATE_TIME_FORMAT.format(new Date(value))
}

function getCounterpartyLabel(item: BookingInboxItem, role?: string) {
  if (role === 'LANDLORD') {
    return item.tenantOrganizationName
  }

  return item.landlordOrganizationName
}

function isActionRequired(item: BookingInboxItem, role?: string) {
  if (role === 'LANDLORD') {
    return item.status === 'REQUESTED'
  }

  if (role === 'TENANT') {
    return item.status === 'AWAITING_CONFIRMATION'
  }

  return false
}

function isConfirmedStatus(status: BookingInboxItem['status']) {
  return status === 'CONFIRMED'
}

function isActiveStatus(status: BookingInboxItem['status']) {
  return status === 'IN_PROGRESS'
}

function isCompletedStatus(status: BookingInboxItem['status']) {
  return (
    status === 'COMPLETED' ||
    status === 'REJECTED' ||
    status === 'CANCELLED' ||
    status === 'EXPIRED'
  )
}

export function BookingInboxPage() {
  const { profile } = useAuth()
  const location = useLocation()
  const [items, setItems] = useState<BookingInboxItem[]>([])
  const [state, setState] = useState<InboxState>('loading')
  const [message, setMessage] = useState('')
  const [refreshing, setRefreshing] = useState(false)
  const [requestPending, setRequestPending] = useState(false)
  const [view, setView] = useState<InboxView>('all')
  const pollTimeoutRef = useRef<number | null>(null)
  const requestInFlightRef = useRef(false)
  const mountedRef = useRef(false)

  const visibleItems = useMemo(() => {
    return items.filter((item) => {
      if (view === 'action-required') {
        return isActionRequired(item, profile?.role)
      }

      if (view === 'confirmed') {
        return isConfirmedStatus(item.status)
      }

      if (view === 'active') {
        return isActiveStatus(item.status)
      }

      if (view === 'completed') {
        return isCompletedStatus(item.status)
      }

      return true
    })
  }, [items, profile?.role, view])

  function clearPolling() {
    if (pollTimeoutRef.current !== null) {
      window.clearTimeout(pollTimeoutRef.current)
      pollTimeoutRef.current = null
    }
  }

  function schedulePolling() {
    clearPolling()
    pollTimeoutRef.current = window.setTimeout(() => {
      void pollInbox()
    }, POLL_INTERVAL_MS)
  }

  async function pollInbox() {
    if (requestInFlightRef.current) {
      return
    }

    requestInFlightRef.current = true
    setRequestPending(true)

    try {
      const inbox = await getBookingInbox()

      if (!mountedRef.current) {
        return
      }

      setItems(inbox)
      setState(inbox.length === 0 ? 'empty' : 'loaded')
      setMessage('')
    } catch (error) {
      if (!mountedRef.current) {
        return
      }

      const parsedError = parseBookingApiError(error)
      setMessage(parsedError.message || 'Не удалось обновить список заявок.')
    } finally {
      requestInFlightRef.current = false

      if (mountedRef.current) {
        setRequestPending(false)
        schedulePolling()
      }
    }
  }

  async function handleRefresh() {
    if (requestInFlightRef.current) {
      return
    }

    requestInFlightRef.current = true
    setRefreshing(true)
    setRequestPending(true)

    try {
      const inbox = await getBookingInbox()

      if (!mountedRef.current) {
        return
      }

      setItems(inbox)
      setState(inbox.length === 0 ? 'empty' : 'loaded')
      setMessage('')
    } catch (error) {
      if (!mountedRef.current) {
        return
      }

      const parsedError = parseBookingApiError(error)
      setMessage(parsedError.message || 'Не удалось обновить список заявок.')
    } finally {
      requestInFlightRef.current = false

      if (mountedRef.current) {
        setRefreshing(false)
        setRequestPending(false)
        schedulePolling()
      }
    }
  }

  useEffect(() => {
    mountedRef.current = true

    return () => {
      mountedRef.current = false
      clearPolling()
    }
  }, [])

  /* eslint-disable react-hooks/exhaustive-deps */
  useEffect(() => {
    let cancelled = false

    async function loadInitialInbox() {
      clearPolling()

      if (requestInFlightRef.current) {
        return
      }

      requestInFlightRef.current = true
      setRequestPending(true)
      setState('loading')
      setMessage('')

      try {
        const inbox = await getBookingInbox()

        if (cancelled || !mountedRef.current) {
          return
        }

        setItems(inbox)
        setState(inbox.length === 0 ? 'empty' : 'loaded')
      } catch (error) {
        if (cancelled || !mountedRef.current) {
          return
        }

        const parsedError = parseBookingApiError(error)
        setItems([])
        setState('error')
        setMessage(parsedError.message || 'Не удалось загрузить список заявок.')
      } finally {
        requestInFlightRef.current = false

        if (!cancelled && mountedRef.current) {
          setRequestPending(false)
          setRefreshing(false)
          clearPolling()
          pollTimeoutRef.current = window.setTimeout(() => {
            if (requestInFlightRef.current) {
              return
            }

            requestInFlightRef.current = true
            setRequestPending(true)

            void getBookingInbox()
              .then((inbox) => {
                if (!mountedRef.current) {
                  return
                }

                setItems(inbox)
                setState(inbox.length === 0 ? 'empty' : 'loaded')
                setMessage('')
              })
              .catch((pollError: unknown) => {
                if (!mountedRef.current) {
                  return
                }

                const parsedError = parseBookingApiError(pollError)
                setMessage(parsedError.message || 'Не удалось обновить список заявок.')
              })
              .finally(() => {
                requestInFlightRef.current = false

                if (mountedRef.current) {
                  setRequestPending(false)
                  clearPolling()
                  pollTimeoutRef.current = window.setTimeout(() => {
                    if (requestInFlightRef.current) {
                      return
                    }

                    requestInFlightRef.current = true
                    setRequestPending(true)

                    void getBookingInbox()
                      .then((nextInbox) => {
                        if (!mountedRef.current) {
                          return
                        }

                        setItems(nextInbox)
                        setState(nextInbox.length === 0 ? 'empty' : 'loaded')
                        setMessage('')
                      })
                      .catch((nextPollError: unknown) => {
                        if (!mountedRef.current) {
                          return
                        }

                        const parsedError = parseBookingApiError(nextPollError)
                        setMessage(parsedError.message || 'Не удалось обновить список заявок.')
                      })
                      .finally(() => {
                        requestInFlightRef.current = false

                        if (mountedRef.current) {
                          setRequestPending(false)
                          schedulePolling()
                        }
                      })
                  }, POLL_INTERVAL_MS)
                }
              })
          }, POLL_INTERVAL_MS)
        }
      }
    }

    void loadInitialInbox()

    return () => {
      cancelled = true
      clearPolling()
    }
  }, [location.key])
  /* eslint-enable react-hooks/exhaustive-deps */

  const title =
    profile?.role === 'LANDLORD'
      ? 'Полученные заявки'
      : profile?.role === 'TENANT'
        ? 'Отправленные заявки'
        : 'Заявки на бронирование'

  const actionRequiredCount = items.filter((item) => isActionRequired(item, profile?.role)).length
  const confirmedCount = items.filter((item) => isConfirmedStatus(item.status)).length
  const activeCount = items.filter((item) => isActiveStatus(item.status)).length
  const completedCount = items.filter((item) => isCompletedStatus(item.status)).length

  return (
    <main className={styles.page}>
      <Link to="/" className={styles.backLink}>
        ← К каталогу
      </Link>

      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>{title}</h1>
          <p className={styles.subtitle}>
            Список заявок, которые доступны текущему аккаунту. Статусы обновляются автоматически,
            а по клику можно открыть полную карточку заявки.
          </p>
        </div>

        <div className={styles.headerActions}>
          <div className={styles.segmentedControl} role="tablist" aria-label="Фильтр заявок">
            <button
              type="button"
              className={`${styles.segmentButton} ${view === 'action-required' ? styles.segmentButtonActive : ''}`}
              onClick={() => setView('action-required')}
              aria-pressed={view === 'action-required'}
            >
              Требуют действия
            </button>
            <button
              type="button"
              className={`${styles.segmentButton} ${view === 'confirmed' ? styles.segmentButtonActive : ''}`}
              onClick={() => setView('confirmed')}
              aria-pressed={view === 'confirmed'}
            >
              Подтвержденные
            </button>
            <button
              type="button"
              className={`${styles.segmentButton} ${view === 'active' ? styles.segmentButtonActive : ''}`}
              onClick={() => setView('active')}
              aria-pressed={view === 'active'}
            >
              Активные
            </button>
            <button
              type="button"
              className={`${styles.segmentButton} ${view === 'completed' ? styles.segmentButtonActive : ''}`}
              onClick={() => setView('completed')}
              aria-pressed={view === 'completed'}
            >
              Завершенные
            </button>
            <button
              type="button"
              className={`${styles.segmentButton} ${view === 'all' ? styles.segmentButtonActive : ''}`}
              onClick={() => setView('all')}
              aria-pressed={view === 'all'}
            >
              Все
            </button>
          </div>

          <button
            type="button"
            className={styles.refreshButton}
            onClick={() => void handleRefresh()}
            disabled={requestPending}
          >
            {refreshing ? 'Обновление...' : 'Обновить'}
          </button>
        </div>
      </div>

      {state === 'loading' && <div className={styles.stateBox}>Загрузка заявок...</div>}

      {state === 'error' && (
        <div className={`${styles.stateBox} ${styles.error}`}>
          <h2 className={styles.stateTitle}>Не удалось загрузить заявки</h2>
          <p className={styles.stateText}>{message}</p>
          <button
            type="button"
            className={styles.retryButton}
            onClick={() => void handleRefresh()}
            disabled={requestPending}
          >
            Повторить
          </button>
        </div>
      )}

      {state === 'empty' && (
        <div className={styles.stateBox}>
          <h2 className={styles.stateTitle}>Заявок пока нет</h2>
          <p className={styles.stateText}>
            Здесь появятся ваши заявки на бронирование, когда они будут созданы или получены.
          </p>
        </div>
      )}

      {state === 'loaded' && (
        <section className={styles.list} aria-label="Список заявок">
          <div className={styles.summary}>
            <span>Всего: {items.length}</span>
            <span>Требуют действия: {actionRequiredCount}</span>
            <span>Подтвержденные: {confirmedCount}</span>
            <span>Активные: {activeCount}</span>
            <span>Завершенные: {completedCount}</span>
          </div>

          {message && <div className={`${styles.stateBox} ${styles.warning}`}>{message}</div>}

          {visibleItems.map((item) => (
            <article key={item.id} className={styles.card}>
              <div
                className={`${styles.cardHeader} ${isActionRequired(item, profile?.role) ? styles.cardHeaderUrgent : ''}`}
              >
                <div>
                  <h2 className={styles.cardTitle}>{item.listingTitle}</h2>
                  <p className={styles.cardMeta}>
                    #{item.id} · {getCounterpartyLabel(item, profile?.role)}
                  </p>
                </div>
                <span
                  className={`${styles.status} ${isActionRequired(item, profile?.role) ? styles.statusUrgent : ''}`}
                >
                  {BOOKING_STATUS_LABELS[item.status]}
                </span>
              </div>

              <dl className={styles.details}>
                <div>
                  <dt>Период</dt>
                  <dd>
                    {formatDateTime(item.startAt)} - {formatDateTime(item.endAt)}
                  </dd>
                </div>
                <div>
                  <dt>Итог</dt>
                  <dd>{item.totalPrice.toLocaleString('ru-RU')} ₽</dd>
                </div>
                <div>
                  <dt>Обновлено</dt>
                  <dd>{formatDateTime(item.updatedAt)}</dd>
                </div>
                <div>
                  <dt>Создано</dt>
                  <dd>{formatDateTime(item.createdAt)}</dd>
                </div>
              </dl>

              <div className={styles.actions}>
                <Link
                  to={`/bookings/${item.id}`}
                  state={{ from: '/bookings' }}
                  className={styles.openLink}
                >
                  Открыть заявку
                </Link>
              </div>
            </article>
          ))}
        </section>
      )}

      {state === 'loaded' && visibleItems.length === 0 && (
        <div className={styles.stateBox}>
          <h2 className={styles.stateTitle}>В этом разделе пока нет заявок</h2>
          <p className={styles.stateText}>
            Переключите вкладку или дождитесь обновления, если статус заявок изменился.
          </p>
        </div>
      )}
    </main>
  )
}
