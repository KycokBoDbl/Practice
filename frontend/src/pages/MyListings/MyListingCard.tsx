import { Link } from 'react-router-dom'

import type { OwnedListing } from '../../types/listing'
import { getSpaceTypeLabel } from '../../types/spaceType'
import {
  formatPrice,
  normalizeText,
  STATUS_LABELS,
  type ListingAction,
} from './myListingsHelpers'
import styles from './MyListingsPage.module.css'

interface MyListingCardProps {
  listing: OwnedListing
  pendingAction: { listingId: number; action: ListingAction } | null
  onEdit: (listing: OwnedListing) => void
  onAction: (listing: OwnedListing, action: ListingAction) => void
}

export function MyListingCard({
  listing,
  onAction,
  onEdit,
  pendingAction,
}: MyListingCardProps) {
  const isPending = (action: ListingAction) =>
    pendingAction !== null &&
    pendingAction.listingId === listing.id &&
    pendingAction.action === action

  return (
    <article className={styles.card}>
      <div className={styles.cardHeader}>
        <div>
          <h2 className={styles.cardTitle}>{listing.title}</h2>
          <p className={styles.cardMeta}>
            {listing.city} • {listing.address}
          </p>
        </div>
        <span className={styles.status}>{STATUS_LABELS[listing.status]}</span>
      </div>

      <dl className={styles.details}>
        <div>
          <dt>Владелец</dt>
          <dd>{normalizeText(listing.ownerOrganizationName)}</dd>
        </div>
        <div>
          <dt>Тип</dt>
          <dd>{getSpaceTypeLabel(listing.spaceType)}</dd>
        </div>
        <div>
          <dt>Вместимость</dt>
          <dd>до {listing.capacity} человек</dd>
        </div>
        <div>
          <dt>Цена</dt>
          <dd>{formatPrice(listing.pricePerHour)} / час</dd>
        </div>
      </dl>

      <p className={styles.description}>
        {listing.description?.trim() || 'Описание пока не добавлено.'}
      </p>

      <div className={styles.actionsRow}>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => onEdit(listing)}
          disabled={Boolean(pendingAction)}
        >
          Редактировать
        </button>
        {listing.status === 'PUBLISHED' ? (
          <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => onAction(listing, 'hide')}
          disabled={isPending('hide')}
          >
            Скрыть
          </button>
        ) : (
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => onAction(listing, 'activate')}
            disabled={isPending('activate')}
          >
            Опубликовать снова
          </button>
        )}
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => onAction(listing, 'delete')}
          disabled={isPending('delete')}
        >
          Удалить
        </button>
        <Link to={`/spaces/${listing.id}`} className={styles.linkButton}>
          Открыть в каталоге
        </Link>
      </div>
    </article>
  )
}
