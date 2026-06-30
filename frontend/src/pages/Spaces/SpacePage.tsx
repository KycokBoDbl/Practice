import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { getListings } from '../../api/listings'
import type { Listing } from '../../types/listing'
import { SPACE_TYPE_LABELS } from '../../types/spaceType'
import styles from './SpacePage.module.css'
import { BookingCalendar } from '../../components/BookingCalendar/BookingCalendar'

export function SpacePage() {
  const { id } = useParams()
  const [listing, setListing] = useState<Listing | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadListing() {
      try {
        const data = await getListings()
        const foundListing = data.find((item) => String(item.id) === id)

        setListing(foundListing ?? null)
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

  return (
    <main className={styles.page}>
      <Link to="/spaces" className={styles.backLink}>
        ← Вернуться на главную
      </Link>

      <div className={styles.layout}>
        <section>
          <div className={styles.gallery}>
            <img
              className={styles.mainImage}
              src={listing.imageUrl}
              alt={listing.title}
            />
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
                  {SPACE_TYPE_LABELS[listing.spaceType] ?? listing.spaceType}
                </p>
              </div>
            </div>

            <h2>Описание</h2>
            <p className={styles.description}>{listing.description}</p>
          </div>
        </section>

        <section className={styles.calendarSection}>
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
        </section>
      </div>
    </main>
  )
}
