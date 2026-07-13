import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { useAuth } from '../../auth/useAuth'
import { getSpaceTypeLabel } from '../../types/spaceType'
import { CatalogSearch } from './CatalogSearch'
import { filterCatalogListings, parseCatalogQuery } from './catalogFilters'
import styles from './SpacesPage.module.css'
import { useListingsPolling } from './useListingsPolling'

export function SpacesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { isAuthenticated, loading: authLoading } = useAuth()
  const { listings, loading } = useListingsPolling()
  const catalogQuery = useMemo(
    () => parseCatalogQuery(searchParams),
    [searchParams],
  )
  const showProjectBanner = !authLoading && !isAuthenticated

  const filteredListings = useMemo(
    () => filterCatalogListings(listings, catalogQuery),
    [catalogQuery, listings],
  )

  function resetFilters() {
    setSearchParams({})
  }

  if (loading) {
    return <p>Загрузка помещений...</p>
  }

  return (
    <main className={styles.page}>
      {showProjectBanner && (
        <div className={styles.hero}>
          <div>
            <h1 className={styles.title}>Помещения для бизнеса и мероприятий</h1>
            <p className={styles.subtitle}>
              Просматривайте доступные пространства, изучайте подробную
              информацию и выбирайте подходящую площадку для встреч, обучения и
              корпоративных мероприятий.
            </p>
          </div>
          <div className={styles.features}>
            <div className={styles.feature}>
              <span className={styles.featureIcon}>🏢</span>
              <div>
                <h3>Разные типы помещений</h3>
                <p>Переговорные, конференц-залы, классы, лофты и шоурумы.</p>
              </div>
            </div>

            <div className={styles.feature}>
              <span className={styles.featureIcon}>📍</span>
              <div>
                <h3>Несколько городов</h3>
                <p>
                  Выбирайте площадки в Москве, Санкт-Петербурге, Казани и
                  других городах.
                </p>
              </div>
            </div>

            <div className={styles.feature}>
              <span className={styles.featureIcon}>🕒</span>
              <div>
                <h3>Почасовая аренда</h3>
                <p>
                  Сравнивайте стоимость и подбирайте помещение под нужное время.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      <section className={styles.catalogHeader} id="catalog">
        <div>
          <h2>Каталог помещений</h2>
          <p>
            Выберите подходящее пространство и перейдите к подробному описанию.
          </p>
        </div>
      </section>

      <CatalogSearch listings={listings} />

      {listings.length === 0 ? (
        <p>Помещений пока нет.</p>
      ) : filteredListings.length === 0 ? (
        <div className={styles.emptyState}>
          <p>По выбранным фильтрам помещений нет.</p>
          <button type="button" className={styles.resetButton} onClick={resetFilters}>
            Сбросить фильтры
          </button>
        </div>
      ) : (
        <section className={styles.grid}>
          {filteredListings.map((listing) => (
            <Link
              key={listing.id}
              to={`/spaces/${listing.id}`}
              className={styles.card}
            >
              {listing.imageUrl?.trim() ? (
                <img
                  className={styles.image}
                  src={listing.imageUrl}
                  alt={listing.title}
                />
              ) : (
                <div className={styles.imagePlaceholder} aria-hidden="true">
                  <span>Нет изображения</span>
                </div>
              )}

              <div className={styles.content}>
                <h2 className={styles.cardTitle}>{listing.title}</h2>

                <p className={styles.meta}>📍 {listing.city}</p>
                <p className={styles.meta}>👥 до {listing.capacity} человек</p>
                <p className={styles.meta}>
                  🏢 {getSpaceTypeLabel(listing.spaceType)}
                </p>

                <div className={styles.price}>
                  {listing.pricePerHour.toLocaleString('ru-RU')} ₽/час
                </div>
              </div>
            </Link>
          ))}
        </section>
      )}
    </main>
  )
}
