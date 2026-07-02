import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { getListing } from '../../api/listings'
import { BookingCalendar } from '../../components/BookingCalendar/BookingCalendar'
import type { Listing } from '../../types/listing'
import styles from './BookingPage.module.css'

type ListingLoadStatus = 'loading' | 'loaded' | 'notFound' | 'error'

export function BookingPage() {
  const { id } = useParams()
  const [listing, setListing] = useState<Listing | null>(null)
  const [status, setStatus] = useState<ListingLoadStatus>('loading')

  useEffect(() => {
    let cancelled = false

    async function loadListing() {
      setStatus('loading')
      setListing(null)

      try {
        const foundListing = await getListing(id)

        if (cancelled) {
          return
        }

        setListing(foundListing ?? null)
        setStatus(foundListing ? 'loaded' : 'notFound')
      } catch (error) {
        console.error(error)

        if (!cancelled) {
          setListing(null)
          setStatus('error')
        }
      }
    }

    loadListing()

    return () => {
      cancelled = true
    }
  }, [id])

  if (status === 'loading') {
    return (
      <main className={styles.page}>
        <h1>Загрузка помещения...</h1>
      </main>
    )
  }

  if (status === 'error') {
    return (
      <main className={styles.page}>
        <h1>Не удалось загрузить помещение</h1>
      </main>
    )
  }

  if (status === 'notFound' || !listing) {
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

        <BookingCalendar
          listingId={listing.id}
          pricePerHour={listing.pricePerHour}
        />
      </section>
    </main>
  )
}
