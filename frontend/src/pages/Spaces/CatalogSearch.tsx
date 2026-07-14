import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import type { Listing } from '../../types/listing'
import styles from './SpacesPage.module.css'
import {
  emptyCatalogQuery,
  getActiveCatalogFilterBadges,
  getCatalogCityOptions,
  isCatalogQueryEmpty,
  parseCatalogQuery,
  setCatalogQueryParams,
  type CatalogQuery,
  type CatalogSearchParamName,
} from './catalogFilters'

interface CatalogSearchProps {
  aiActive: boolean
  aiLoading: boolean
  aiMessage: string
  aiPrompt: string
  aiResultCount: number
  listings: Listing[]
  onAiSearch: (prompt: string) => void
  onClearAiSearch: () => void
}

export function CatalogSearch({
  aiActive,
  aiLoading,
  aiMessage,
  aiPrompt,
  aiResultCount,
  listings,
  onAiSearch,
  onClearAiSearch,
}: CatalogSearchProps) {
  const [searchParams] = useSearchParams()
  const committedQuery = useMemo(
    () => parseCatalogQuery(searchParams),
    [searchParams],
  )

  return (
    <CatalogSearchForm
      key={searchParams.toString()}
      aiActive={aiActive}
      aiLoading={aiLoading}
      aiMessage={aiMessage}
      aiPrompt={aiPrompt}
      aiResultCount={aiResultCount}
      committedQuery={committedQuery}
      listings={listings}
      onAiSearch={onAiSearch}
      onClearAiSearch={onClearAiSearch}
      searchParams={searchParams}
    />
  )
}

interface CatalogSearchFormProps {
  aiActive: boolean
  aiLoading: boolean
  aiMessage: string
  aiPrompt: string
  aiResultCount: number
  committedQuery: CatalogQuery
  listings: Listing[]
  onAiSearch: (prompt: string) => void
  onClearAiSearch: () => void
  searchParams: URLSearchParams
}

function CatalogSearchForm({
  aiActive,
  aiLoading,
  aiMessage,
  aiPrompt,
  aiResultCount,
  committedQuery,
  listings,
  onAiSearch,
  onClearAiSearch,
  searchParams,
}: CatalogSearchFormProps) {
  const navigate = useNavigate()
  const [draftQuery, setDraftQuery] = useState(committedQuery)
  const [filtersOpen, setFiltersOpen] = useState(false)
  const searchAreaRef = useRef<HTMLDivElement>(null)
  const cities = useMemo(() => getCatalogCityOptions(listings), [listings])
  const activeFilterBadges = useMemo(
    () => getActiveCatalogFilterBadges(committedQuery),
    [committedQuery],
  )
  const hasActiveFilters = activeFilterBadges.length > 0
  const filtersVisible = filtersOpen || hasActiveFilters

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      if (!searchAreaRef.current?.contains(event.target as Node)) {
        setFiltersOpen(false)
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
    }
  }, [])

  function updateDraft(name: keyof CatalogQuery, value: string) {
    setDraftQuery((current) => ({
      ...current,
      [name]: value,
    }))
  }

  function updateRangeMin(
    minName: keyof CatalogQuery,
    maxName: keyof CatalogQuery,
    value: string,
  ) {
    setDraftQuery((current) => ({
      ...current,
      [minName]: value,
      [maxName]:
        value !== '' &&
        current[maxName] !== '' &&
        Number(value) > Number(current[maxName])
          ? value
          : current[maxName],
    }))
  }

  function updateRangeMax(
    minName: keyof CatalogQuery,
    maxName: keyof CatalogQuery,
    value: string,
  ) {
    setDraftQuery((current) => ({
      ...current,
      [minName]:
        value !== '' &&
        current[minName] !== '' &&
        Number(value) < Number(current[minName])
          ? value
          : current[minName],
      [maxName]: value,
    }))
  }

  function navigateWithQuery(query: CatalogQuery) {
    const nextParams = setCatalogQueryParams(searchParams, query)
    const hasNextFilters = !isCatalogQueryEmpty(query)

    setFiltersOpen(hasNextFilters)

    navigate({
      pathname: '/',
      search: nextParams.toString(),
      hash: hasNextFilters ? 'catalog' : undefined,
    })
  }

  function runSearch() {
    if (aiActive) {
      onClearAiSearch()
    }

    navigateWithQuery(draftQuery)
  }

  function resetFilters() {
    setDraftQuery(emptyCatalogQuery)
    navigateWithQuery(emptyCatalogQuery)
  }

  function removeFilter(name: CatalogSearchParamName) {
    const nextQuery = {
      ...committedQuery,
      [name]: '',
    }

    setDraftQuery(nextQuery)
    navigateWithQuery(nextQuery)
  }

  function submitAiSearch() {
    onAiSearch(draftQuery.q)
  }

  return (
    <div className={styles.searchArea} ref={searchAreaRef}>
      <div className={styles.searchBar}>
        <div className={styles.searchInputWrap}>
          <input
            className={styles.searchInput}
            type="search"
            placeholder="Поиск помещений..."
            aria-label="Поиск помещений"
            value={draftQuery.q}
            onChange={(event) => updateDraft('q', event.target.value)}
            onFocus={() => setFiltersOpen(true)}
          />
          <span className={styles.searchHint}>AI</span>
        </div>

        <button
          type="button"
          className={styles.aiSearchButton}
          onClick={submitAiSearch}
          disabled={aiLoading}
        >
          {aiLoading ? 'Ищем...' : 'AI-поиск'}
        </button>

        <button
          type="button"
          className={styles.searchButton}
          onClick={runSearch}
        >
          Поиск
        </button>
      </div>

      <p className={styles.searchCaption}>
        Строка поиска поддерживает обычный каталог и AI-подбор по свободному запросу.
      </p>

      {(aiActive || aiMessage) && (
        <div
          className={`${styles.aiSearchStatus} ${aiMessage ? styles.aiSearchStatusError : ''}`}
        >
          {aiActive && (
            <p className={styles.aiSearchSummary}>
              <strong>AI-поиск:</strong> {aiPrompt} · найдено {aiResultCount}
            </p>
          )}

          {aiMessage && <p className={styles.aiSearchMessage}>{aiMessage}</p>}

          {aiActive && (
            <button
              type="button"
              className={styles.aiResetButton}
              onClick={onClearAiSearch}
              disabled={aiLoading}
            >
              Вернуться к обычному каталогу
            </button>
          )}
        </div>
      )}

      {hasActiveFilters && (
        <div className={styles.activeFilters} aria-label="Активные фильтры каталога">
          <span className={styles.activeFiltersLabel}>Поиск по фильтрам:</span>
          <div className={styles.filterBadges}>
            {activeFilterBadges.map((badge) => (
              <button
                key={badge.key}
                type="button"
                className={styles.filterBadge}
                onClick={() => removeFilter(badge.key)}
                aria-label={`Убрать фильтр ${badge.label}`}
              >
                <span>
                  {badge.label}: {badge.value}
                </span>
                <span aria-hidden="true">x</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {filtersVisible && (
        <div className={styles.filters} aria-label="Фильтры каталога">
          <label className={styles.filterField}>
            <span>Город</span>
            <select
              value={draftQuery.city}
              onChange={(event) => updateDraft('city', event.target.value)}
            >
              <option value="">Все города</option>
              {cities.map((city) => (
                <option key={city} value={city}>
                  {city}
                </option>
              ))}
            </select>
          </label>

          <div className={styles.rangeField}>
            <span>Вместимость</span>
            <label>
              <span>до</span>
              <input
                type="number"
                min={draftQuery.minCapacity || '1'}
                inputMode="numeric"
                placeholder="Любая"
                value={draftQuery.maxCapacity}
                onChange={(event) =>
                  updateRangeMax(
                    'minCapacity',
                    'maxCapacity',
                    event.target.value,
                  )
                }
                onWheel={(event) => event.currentTarget.blur()}
              />
            </label>
            <label>
              <span>от</span>
              <input
                type="number"
                min="1"
                max={draftQuery.maxCapacity || undefined}
                inputMode="numeric"
                placeholder="Любая"
                value={draftQuery.minCapacity}
                onChange={(event) =>
                  updateRangeMin(
                    'minCapacity',
                    'maxCapacity',
                    event.target.value,
                  )
                }
                onWheel={(event) => event.currentTarget.blur()}
              />
            </label>
          </div>

          <div className={styles.rangeField}>
            <span>Цена</span>
            <label>
              <span>до</span>
              <input
                type="number"
                min={draftQuery.minPrice || '0'}
                step="1000"
                inputMode="numeric"
                placeholder="Без лимита"
                value={draftQuery.maxPrice}
                onChange={(event) =>
                  updateRangeMax('minPrice', 'maxPrice', event.target.value)
                }
                onWheel={(event) => event.currentTarget.blur()}
              />
            </label>
            <label>
              <span>от</span>
              <input
                type="number"
                min="0"
                max={draftQuery.maxPrice || undefined}
                step="1000"
                inputMode="numeric"
                placeholder="Любая"
                value={draftQuery.minPrice}
                onChange={(event) =>
                  updateRangeMin('minPrice', 'maxPrice', event.target.value)
                }
                onWheel={(event) => event.currentTarget.blur()}
              />
            </label>
          </div>

          <button type="button" className={styles.resetButton} onClick={resetFilters}>
            Сбросить
          </button>
        </div>
      )}
    </div>
  )
}
