import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { BookingCalendar } from '../../components/BookingCalendar/BookingCalendar'
import { getListing } from '../../api/listings'
import type { Listing } from '../../types/listing'
import { getSpaceTypeLabel } from '../../types/spaceType'
import styles from './SpacePage.module.css'

const AMENITIES_PREFIXES = ['Удобства: ', 'РЈРґРѕР±СЃС‚РІР°: ']

function splitDescriptionAndAmenities(value: string | null | undefined) {
  const trimmedValue = value?.trim()

  if (!trimmedValue) {
    return {
      description: '',
      amenities: [] as string[],
    }
  }

  const lines = trimmedValue.split(/\r?\n/)
  const lastLine = lines[lines.length - 1]?.trim()
  const prefix = AMENITIES_PREFIXES.find((item) => lastLine?.startsWith(item))

  if (!lastLine || !prefix || !lastLine.endsWith('.')) {
    return {
      description: trimmedValue,
      amenities: [] as string[],
    }
  }

  const amenities = Array.from(
    new Set(
      lastLine
        .slice(prefix.length, -1)
        .split(',')
        .map((amenity) => amenity.trim())
        .filter(Boolean),
    ),
  )

  return {
    description: lines.slice(0, -1).join('\n').trim(),
    amenities,
  }
}

export function SpacePage() {
  const { id } = useParams()
  const [listing, setListing] = useState<Listing | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function loadListing() {
      try {
        const nextListing = await getListing(id)

        if (!cancelled) {
          setListing(nextListing)
        }
      } catch (error) {
        console.error('Ошибка при загрузке помещения:', error)
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadListing()

    return () => {
      cancelled = true
    }
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
  const { amenities, description } = splitDescriptionAndAmenities(listing.description)

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

            <section className={styles.detailsSection}>
              <h2>Описание</h2>
              <p
                className={`${styles.description} ${description ? '' : styles.descriptionMuted}`}
              >
                {description || 'Описание пока не добавлено.'}
              </p>
            </section>

            {amenities.length > 0 && (
              <section className={styles.detailsSection}>
                <h2>Удобства</h2>
                <div className={styles.amenities}>
                  {amenities.map((amenity) => (
                    <span key={amenity} className={styles.amenityChip}>
                      {amenity}
                    </span>
                  ))}
                </div>
              </section>
            )}
          </div>
        </section>

        <aside className={styles.calendarSection}>
          <BookingCalendar
            listingId={listing.id}
            pricePerHour={listing.pricePerHour}
            mode="preview"
          />

          <Link to={`/booking/${listing.id}`} className={styles.button}>
            Забронировать
          </Link>
        </aside>
      </div>
    </main>
  )
}
