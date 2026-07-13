import { Link } from 'react-router-dom'

import type { BookingInboxItem } from '../../types/booking'
import styles from './BookingInboxPage.module.css'
import {
  BOOKING_STATUS_LABELS,
  formatDateTime,
  getCounterpartyLabel,
  isActionRequired,
  type ParticipantRole,
} from './bookingInboxHelpers'

interface BookingInboxCardProps {
  item: BookingInboxItem
  role?: ParticipantRole
}

export function BookingInboxCard({ item, role }: BookingInboxCardProps) {
  const actionRequired = isActionRequired(item, role)

  return (
    <article className={styles.card}>
      <div
        className={`${styles.cardHeader} ${actionRequired ? styles.cardHeaderUrgent : ''}`}
      >
        <div>
          <h2 className={styles.cardTitle}>{item.listingTitle}</h2>
          <p className={styles.cardMeta}>
            #{item.id} · {getCounterpartyLabel(item, role)}
          </p>
        </div>
        <span className={`${styles.status} ${actionRequired ? styles.statusUrgent : ''}`}>
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
        <Link to={`/bookings/${item.id}`} state={{ from: '/bookings' }} className={styles.openLink}>
          Открыть заявку
        </Link>
      </div>
    </article>
  )
}
