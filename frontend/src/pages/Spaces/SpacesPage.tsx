import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import {
  aiSearchListings,
  parseAiListingSearchApiError,
} from '../../api/listings'
import { useAuth } from '../../auth/useAuth'
import type { Listing } from '../../types/listing'
import { getSpaceTypeLabel } from '../../types/spaceType'
import { CatalogSearch } from './CatalogSearch'
import { filterCatalogListings, parseCatalogQuery } from './catalogFilters'
import styles from './SpacesPage.module.css'
import { useListingsPolling } from './useListingsPolling'

const MAX_AI_PROMPT_LENGTH = 1000

export function SpacesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { isAuthenticated, loading: authLoading } = useAuth()
  const { errorMessage, listings, loading } = useListingsPolling()
  const [aiResults, setAiResults] = useState<Listing[]>([])
  const [aiActive, setAiActive] = useState(false)
  const [aiLoading, setAiLoading] = useState(false)
  const [aiMessage, setAiMessage] = useState('')
  const [aiPrompt, setAiPrompt] = useState('')
  const catalogQuery = useMemo(
    () => parseCatalogQuery(searchParams),
    [searchParams],
  )
  const showProjectBanner = !authLoading && !isAuthenticated

  const filteredListings = useMemo(
    () => filterCatalogListings(listings, catalogQuery),
    [catalogQuery, listings],
  )
  const visibleListings = aiActive ? aiResults : filteredListings

  function resetFilters() {
    setSearchParams({})
  }

  async function handleAiSearch(prompt: string) {
    const normalizedPrompt = prompt.trim()

    if (!normalizedPrompt) {
      setAiMessage('Введите текст запроса для AI-поиска.')
      return
    }

    if (normalizedPrompt.length > MAX_AI_PROMPT_LENGTH) {
      setAiMessage('Запрос не должен превышать 1000 символов.')
      return
    }

    if (aiLoading) {
      return
    }

    setAiLoading(true)
    setAiMessage('')

    try {
      const nextResults = await aiSearchListings({ prompt: normalizedPrompt })

      setAiPrompt(normalizedPrompt)
      setAiResults(nextResults)
      setAiActive(true)
    } catch (error) {
      const parsedError = parseAiListingSearchApiError(error)

      setAiMessage(
        parsedError.kind === 'validation'
          ? parsedError.message || 'Проверьте текст запроса и попробуйте снова.'
          : parsedError.kind === 'unavailable'
            ? 'AI-поиск временно недоступен. Обычный каталог продолжает работать.'
            : parsedError.message || 'Не удалось выполнить AI-поиск.',
      )
    } finally {
      setAiLoading(false)
    }
  }

  function clearAiSearch() {
    if (aiLoading) {
      return
    }

    setAiActive(false)
    setAiResults([])
    setAiPrompt('')
    setAiMessage('')
  }

  if (loading) {
    return <p>Загрузка помещений...</p>
  }

  if (errorMessage && listings.length === 0) {
    return (
      <main className={styles.page}>
        <div className={styles.emptyState}>
          <p>{errorMessage}</p>
        </div>
      </main>
    )
  }

  return (
    <main className={styles.page}>
      {showProjectBanner && (
        <div className={styles.hero}>
          <div>
            <h1 className={styles.title}>Помещения для бизнеса и мероприятий</h1>
            <p className={styles.subtitle}>
              Просматривайте доступные пространства, изучайте подробную информацию и выбирайте
              подходящую площадку для встреч, обучения и корпоративных мероприятий.
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
                <p>Выбирайте площадки в Москве, Санкт-Петербурге, Казани и других городах.</p>
              </div>
            </div>

            <div className={styles.feature}>
              <span className={styles.featureIcon}>⏱</span>
              <div>
                <h3>Почасовая аренда</h3>
                <p>Сравнивайте стоимость и подбирайте помещение под нужное время.</p>
              </div>
            </div>
          </div>
        </div>
      )}

      <section className={styles.catalogHeader} id="catalog">
        <div>
          <h2>Каталог помещений</h2>
          <p>Выберите подходящее пространство и перейдите к подробному описанию.</p>
        </div>
      </section>

      <CatalogSearch
        aiActive={aiActive}
        aiLoading={aiLoading}
        aiMessage={aiMessage}
        aiPrompt={aiPrompt}
        aiResultCount={aiResults.length}
        listings={listings}
        onAiSearch={handleAiSearch}
        onClearAiSearch={clearAiSearch}
      />

      {errorMessage && (
        <div className={styles.emptyState}>
          <p>{errorMessage}</p>
        </div>
      )}

      {!aiActive && listings.length === 0 ? (
        <p>Помещений пока нет.</p>
      ) : visibleListings.length === 0 ? (
        <div className={styles.emptyState}>
          <p>
            {aiActive
              ? `По запросу "${aiPrompt}" подходящих помещений не найдено.`
              : 'По выбранным фильтрам помещений нет.'}
          </p>
          <button
            type="button"
            className={styles.resetButton}
            onClick={aiActive ? clearAiSearch : resetFilters}
          >
            {aiActive ? 'Вернуться к каталогу' : 'Сбросить фильтры'}
          </button>
        </div>
      ) : (
        <section className={styles.grid}>
          {visibleListings.map((listing) => (
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
                <p className={styles.meta}>🏢 {getSpaceTypeLabel(listing.spaceType)}</p>

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
