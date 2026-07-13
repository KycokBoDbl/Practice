import { Link } from 'react-router-dom'

import type { Listing } from '../../types/listing'
import { getSpaceTypeLabel } from '../../types/spaceType'
import type { PublicationFormState } from './listingPublicationForm'
import styles from './ListingPublicationPage.module.css'

interface PublicationPreviewProps {
  createdListing: Listing | null
  error: string | undefined
  form: PublicationFormState
  imageUrl: string
  ownerName: string
  previewDescription: string | null
  pricePreview: string
  selectedAmenities: string[]
  canPreviewImage: boolean
}

export function PublicationPreview({
  canPreviewImage,
  createdListing,
  error,
  form,
  imageUrl,
  ownerName,
  previewDescription,
  pricePreview,
  selectedAmenities,
}: PublicationPreviewProps) {
  return (
    <aside className={styles.previewColumn}>
      <div className={styles.previewFrame}>
        <div className={styles.previewImage}>
          {canPreviewImage ? (
            <img src={imageUrl} alt={form.title || 'Изображение помещения'} />
          ) : (
            <div className={styles.imagePlaceholder}>Изображение не добавлено</div>
          )}
        </div>

        <div className={styles.previewBody}>
          <div className={styles.previewTopline}>
            <span className={styles.previewBadge}>Превью</span>
            <span className={styles.previewBadgeMuted}>Будет опубликовано сразу</span>
          </div>

          <h2 className={styles.previewTitle}>
            {form.title.trim() || 'Название помещения появится здесь'}
          </h2>

          <p className={styles.previewMeta}>
            {form.city.trim() || 'Город'} • {form.address.trim() || 'Адрес'}
          </p>

          <div className={styles.previewPrice}>{pricePreview}</div>

          <dl className={styles.previewFacts}>
            <div>
              <dt>Владелец</dt>
              <dd>{ownerName}</dd>
            </div>
            <div>
              <dt>Тип</dt>
              <dd>{getSpaceTypeLabel(form.spaceType)}</dd>
            </div>
            <div>
              <dt>Вместимость</dt>
              <dd>{form.capacity.trim() ? `до ${form.capacity.trim()} человек` : 'Не указана'}</dd>
            </div>
            <div>
              <dt>Статус</dt>
              <dd>Новое объявление</dd>
            </div>
          </dl>

          <section className={styles.previewSection}>
            <h3>Описание</h3>
            <p className={styles.previewDescription}>
              {previewDescription || 'Описание и удобства появятся здесь после заполнения.'}
            </p>
          </section>

          <section className={styles.previewSection}>
            <h3>Удобства</h3>
            <div className={styles.previewChips}>
              {selectedAmenities.length > 0 ? (
                selectedAmenities.map((amenity) => (
                  <span key={amenity} className={styles.previewChip}>
                    {amenity}
                  </span>
                ))
              ) : (
                <span className={styles.previewHint}>Теги пока не выбраны</span>
              )}
            </div>
          </section>

          {error && (
            <p className={styles.error} role="alert">
              {error}
            </p>
          )}

          {createdListing && (
            <div className={styles.success} role="status">
              <p>Объявление опубликовано.</p>
              <p>Владелец: {createdListing.ownerOrganizationName || ownerName}</p>
              <Link to={`/spaces/${createdListing.id}`}>Открыть объявление</Link>
            </div>
          )}

        </div>
      </div>
    </aside>
  )
}
