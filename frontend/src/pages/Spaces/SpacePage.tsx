import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { getListing } from '../../api/listings'
import type { Listing } from '../../types/listing'
import { getSpaceTypeLabel } from '../../types/spaceType'
import styles from './SpacePage.module.css'
import { BookingCalendar } from '../../components/BookingCalendar/BookingCalendar'

export function SpacePage() {
  const { id } = useParams()
  const [listing, setListing] = useState<Listing | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadListing() {
      try {
        setListing(await getListing(id))
      } catch (error) {
        console.error('Ошибка при загрузке помещения:', error)
      } finally {
        setLoading(false)
      }
    }

    loadListing()
  }, [id])

  if (loading) {
    return <main className={styles.page}>Загрузка помещения...</main>
  }

  if (!listing) {
    return (
      <main className={styles.page}>
        <Link to="/" className={styles.backLink}>
          ← Вернуться на главную
        </Link>
        <h1>Помещение не найдено</h1>
      </main>
    )
  }

  const imageUrl = listing.imageUrl?.trim()
  const description = listing.description?.trim()

  return (
    <main className={styles.page}>
      <Link to="/spaces" className={styles.backLink}>
        ← Вернуться на главную
      </Link>

      <div className={styles.layout}>
        <section>
          <div className={styles.gallery}>
            {imageUrl ? (
              <img
                className={styles.mainImage}
                src={imageUrl}
                alt={listing.title}
              />
            ) : (
              <div className={styles.mainImagePlaceholder} aria-hidden="true">
                <span>Изображение не добавлено</span>
              </div>
            )}
          </div>

          <div className={styles.info}>
            <h1 className={styles.title}>{listing.title}</h1>

            <div className={styles.metaGrid}>
              <div className={styles.metaCard}>
                <p className={styles.metaLabel}>Город</p>
                <p className={styles.metaValue}>{listing.city}</p>
              </div>

              <div className={styles.metaCard}>
                <p className={styles.metaLabel}>Адрес</p>
                <p className={styles.metaValue}>{listing.address}</p>
              </div>

              <div className={styles.metaCard}>
                <p className={styles.metaLabel}>Вместимость</p>
                <p className={styles.metaValue}>
                  до {listing.capacity} человек
                </p>
              </div>

              <div className={styles.metaCard}>
                <p className={styles.metaLabel}>Тип помещения</p>
                <p className={styles.metaValue}>
                  {getSpaceTypeLabel(listing.spaceType)}
                </p>
              </div>
            </div>

            <h2>Описание</h2>
            <p
              className={`${styles.description} ${description ? '' : styles.descriptionMuted}`}
            >
              {description || 'Описание пока не добавлено.'}
            </p>
          </div>
        </section>

        <section className={styles.calendarSection}>
            <BookingCalendar
              listingId={listing.id}
              pricePerHour={listing.pricePerHour}
              mode="preview"
            />

            <Link to={`/booking/${listing.id}`} className={styles.button}>
              Забронировать
            </Link>
        </section>
      </div>
    </main>
  )
}
