import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'

import { getBookingInbox, parseBookingApiError } from '../../api/bookings'
import { useAuth } from '../../auth/useAuth'
import type { BookingInboxItem } from '../../types/booking'
import styles from './BookingInboxPage.module.css'

type InboxState = 'loading' | 'loaded' | 'empty' | 'error'
type InboxView = 'all' | 'action-required' | 'active' | 'terminal'

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

function isTerminalStatus(status: BookingInboxItem['status']) {
  return (
    status === 'COMPLETED' ||
    status === 'REJECTED' ||
    status === 'CANCELLED' ||
    status === 'EXPIRED'
  )
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

function getViewLabel(view: InboxView) {
  if (view === 'action-required') return 'Требуют действия'
  if (view === 'active') return 'Активные'
  if (view === 'terminal') return 'Завершенные'
  return 'Все'
}

export function BookingInboxPage() {
  const { profile } = useAuth()
  const location = useLocation()
  const [items, setItems] = useState<BookingInboxItem[]>([])
  const [state, setState] = useState<InboxState>('loading')
  const [message, setMessage] = useState('')
  const [refreshing, setRefreshing] = useState(false)
  const [view, setView] = useState<InboxView>('all')

  const visibleItems = items.filter((item) => {
    if (view === 'action-required') {
      return isActionRequired(item, profile?.role)
    }

    if (view === 'active') {
      return !isTerminalStatus(item.status)
    }

    if (view === 'terminal') {
      return isTerminalStatus(item.status)
    }

    return true
  })

  useEffect(() => {
    let cancelled = false

    async function loadInbox() {
      setState('loading')
      setMessage('')

      try {
        const inbox = await getBookingInbox()

        if (cancelled) {
          return
        }

        setItems(inbox)
        setState(inbox.length === 0 ? 'empty' : 'loaded')
      } catch (error) {
        if (cancelled) {
          return
        }

        const parsedError = parseBookingApiError(error)
        setItems([])
        setState('error')
        setMessage(parsedError.message || 'Не удалось загрузить список заявок.')
      } finally {
        if (!cancelled) {
          setRefreshing(false)
        }
      }
    }

    loadInbox()

    return () => {
      cancelled = true
    }
  }, [location.key])

  async function handleRefresh() {
    setRefreshing(true)

    try {
      const inbox = await getBookingInbox()
      setItems(inbox)
      setState(inbox.length === 0 ? 'empty' : 'loaded')
      setMessage('')
    } catch (error) {
      const parsedError = parseBookingApiError(error)
      setState('error')
      setMessage(parsedError.message || 'Не удалось обновить список заявок.')
    } finally {
      setRefreshing(false)
    }
  }

  const title =
    profile?.role === 'LANDLORD'
      ? 'Полученные заявки'
      : profile?.role === 'TENANT'
        ? 'Отправленные заявки'
        : 'Заявки на бронирование'

  const actionRequiredCount = items.filter((item) => isActionRequired(item, profile?.role)).length
  const activeCount = items.filter((item) => !isTerminalStatus(item.status)).length
  const terminalCount = items.length - activeCount

  return (
    <main className={styles.page}>
      <Link to="/" className={styles.backLink}>
        ← К каталогу
      </Link>

      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>{title}</h1>
          <p className={styles.subtitle}>
            Список заявок, которые доступны текущему аккаунту. Открывайте заявку,
            чтобы увидеть статус и историю.
          </p>
        </div>

        <div className={styles.headerActions}>
          <div className={styles.segmentedControl} role="tablist" aria-label="Фильтр заявок">
            <button
              type="button"
              className={`${styles.segmentButton} ${view === 'all' ? styles.segmentButtonActive : ''}`}
              onClick={() => setView('all')}
              aria-pressed={view === 'all'}
            >
              Все
            </button>
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
              className={`${styles.segmentButton} ${view === 'active' ? styles.segmentButtonActive : ''}`}
              onClick={() => setView('active')}
              aria-pressed={view === 'active'}
            >
              Активные
            </button>
            <button
              type="button"
              className={`${styles.segmentButton} ${view === 'terminal' ? styles.segmentButtonActive : ''}`}
              onClick={() => setView('terminal')}
              aria-pressed={view === 'terminal'}
            >
              Завершенные
            </button>
          </div>

          <button
            type="button"
            className={styles.refreshButton}
            onClick={handleRefresh}
            disabled={state === 'loading' || refreshing}
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
            onClick={handleRefresh}
            disabled={refreshing}
          >
            Повторить
          </button>
        </div>
      )}

      {state === 'empty' && (
        <div className={styles.stateBox}>
          <h2 className={styles.stateTitle}>Заявок пока нет</h2>
          <p className={styles.stateText}>
            Здесь появятся ваши заявки на бронирование, когда они будут созданы или
            получены.
          </p>
        </div>
      )}

      {state === 'loaded' && (
        <section className={styles.list} aria-label="Список заявок">
          <div className={styles.summary}>
            <span>Все: {items.length}</span>
            <span>Требуют действия: {actionRequiredCount}</span>
            <span>Активные: {activeCount}</span>
            <span>Завершенные: {terminalCount}</span>
            <span>Показывается: {getViewLabel(view)}</span>
          </div>

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
            Попробуйте переключить фильтр или обновить список, если статус заявок изменился.
          </p>
        </div>
      )}
    </main>
  )
}
