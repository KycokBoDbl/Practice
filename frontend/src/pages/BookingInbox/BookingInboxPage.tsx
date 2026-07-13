import { useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'

import { useAuth } from '../../auth/useAuth'
import { BookingInboxCard } from './BookingInboxCard'
import styles from './BookingInboxPage.module.css'
import {
  filterInboxItems,
  getInboxSummary,
  getInboxTitle,
  INBOX_VIEWS,
  type InboxView,
} from './bookingInboxHelpers'
import { useBookingInbox } from './useBookingInbox'

export function BookingInboxPage() {
  const { profile } = useAuth()
  const location = useLocation()
  const [view, setView] = useState<InboxView>('all')
  const {
    items,
    message,
    refresh,
    refreshing,
    requestPending,
    state,
  } = useBookingInbox(location.key)

  const visibleItems = useMemo(
    () => filterInboxItems(items, view, profile?.role),
    [items, profile?.role, view],
  )
  const summary = useMemo(
    () => getInboxSummary(items, profile?.role),
    [items, profile?.role],
  )
  const title = getInboxTitle(profile?.role)

  return (
    <main className={styles.page}>
      <Link to="/" className={styles.backLink}>
        ← К каталогу
      </Link>

      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>{title}</h1>
          <p className={styles.subtitle}>
            Список заявок, которые доступны текущему аккаунту. Статусы обновляются
            автоматически, а по клику можно открыть полную карточку заявки.
          </p>
        </div>

        <div className={styles.headerActions}>
          <div className={styles.segmentedControl} role="tablist" aria-label="Фильтр заявок">
            {INBOX_VIEWS.map((item) => (
              <button
                key={item.value}
                type="button"
                className={`${styles.segmentButton} ${view === item.value ? styles.segmentButtonActive : ''}`}
                onClick={() => setView(item.value)}
                aria-pressed={view === item.value}
              >
                {item.label}
              </button>
            ))}
          </div>

          <button
            type="button"
            className={styles.refreshButton}
            onClick={refresh}
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
            onClick={refresh}
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
            <span>Всего: {summary.total}</span>
            <span>Требуют действия: {summary.actionRequired}</span>
            <span>Подтвержденные: {summary.confirmed}</span>
            <span>Активные: {summary.active}</span>
            <span>Завершенные: {summary.completed}</span>
          </div>

          {message && <div className={`${styles.stateBox} ${styles.warning}`}>{message}</div>}

          {visibleItems.map((item) => (
            <BookingInboxCard key={item.id} item={item} role={profile?.role} />
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
