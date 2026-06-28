import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { getListings } from '../../api/listings'
import { BookingCalendar } from '../../components/BookingCalendar/BookingCalendar'
import type { Listing } from '../../types/listing'
import styles from './BookingPage.module.css'

export function BookingPage() {
  const { id } = useParams()
  const [listing, setListing] = useState<Listing | null>(null)

  useEffect(() => {
    async function loadListing() {
      try {
        const data = await getListings()
        const foundListing = data.find((item) => String(item.id) === id)
        setListing(foundListing ?? null)
      } catch (error) {
        console.error(error)
      }
    }

    loadListing()
  }, [id])

  if (!listing) {
    return (
      <main className={styles.page}>
        <h1>Помещение не найдено</h1>
      </main>
    )
  }

  return (
    <main className={styles.page}>
      <Link
        to={`/spaces/${listing.id}`}
        className={styles.backLink}
      >
        ← Вернуться к помещению
      </Link>

      <section className={styles.card}>
        <h1 className={styles.title}>Бронирование помещения</h1>

        <p className={styles.subtitle}>
          <strong>{listing.title}</strong><br />
          {listing.city}, {listing.address}
        </p>

        <BookingCalendar pricePerHour={listing.pricePerHour} />
      </section>
    </main>
  )
}