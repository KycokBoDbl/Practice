import { useEffect, useMemo, useRef, useState } from 'react'
import { NavLink, useLocation, useNavigate, useSearchParams } from 'react-router-dom'

import { getListings } from '../../api/listings'
import type { Listing } from '../../types/listing'
import styles from './Header.module.css'

export function Header() {
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [listings, setListings] = useState<Listing[]>([])
  const [filtersOpen, setFiltersOpen] = useState(false)
  const searchAreaRef = useRef<HTMLDivElement>(null)
  const searchValue = searchParams.get('q') ?? ''
  const selectedCity = searchParams.get('city') ?? ''
  const minCapacity = searchParams.get('minCapacity') ?? ''
  const maxCapacity = searchParams.get('maxCapacity') ?? ''
  const minPrice = searchParams.get('minPrice') ?? ''
  const maxPrice = searchParams.get('maxPrice') ?? ''
  const [draftSearch, setDraftSearch] = useState(searchValue)
  const [draftCity, setDraftCity] = useState(selectedCity)
  const [draftMinCapacity, setDraftMinCapacity] = useState(minCapacity)
  const [draftMaxCapacity, setDraftMaxCapacity] = useState(maxCapacity)
  const [draftMinPrice, setDraftMinPrice] = useState(minPrice)
  const [draftMaxPrice, setDraftMaxPrice] = useState(maxPrice)

  useEffect(() => {
    async function loadListings() {
      try {
        setListings(await getListings())
      } catch (error) {
        console.error('Failed to load listing filters:', error)
      }
    }

    loadListings()
  }, [])

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

  useEffect(() => {
    setDraftSearch(searchValue)
    setDraftCity(selectedCity)
    setDraftMinCapacity(minCapacity)
    setDraftMaxCapacity(maxCapacity)
    setDraftMinPrice(minPrice)
    setDraftMaxPrice(maxPrice)
  }, [maxCapacity, maxPrice, minCapacity, minPrice, searchValue, selectedCity])

  const cities = useMemo(
    () =>
      Array.from(new Set(listings.map((listing) => listing.city)))
        .filter(Boolean)
        .sort((a, b) => a.localeCompare(b, 'ru-RU')),
    [listings],
  )

  function runSearch() {
    const nextParams = new URLSearchParams(searchParams)

    nextParams.delete('filters')
    setParam(nextParams, 'q', draftSearch)
    setParam(nextParams, 'city', draftCity)
    setParam(nextParams, 'minCapacity', draftMinCapacity)
    setParam(nextParams, 'maxCapacity', draftMaxCapacity)
    setParam(nextParams, 'minPrice', draftMinPrice)
    setParam(nextParams, 'maxPrice', draftMaxPrice)
    setFiltersOpen(false)

    navigate({
      pathname: '/',
      search: nextParams.toString(),
      hash: 'catalog',
    })
  }

  function resetFilters() {
    setDraftSearch('')
    setDraftCity('')
    setDraftMinCapacity('')
    setDraftMaxCapacity('')
    setDraftMinPrice('')
    setDraftMaxPrice('')
    setFiltersOpen(false)

    navigate({
      pathname: location.pathname === '/' ? location.pathname : '/',
      search: '',
    })
  }

  function setParam(params: URLSearchParams, name: string, value: string) {
    const nextValue = value.trim()

    if (nextValue) {
      params.set(name, nextValue)
    } else {
      params.delete(name)
    }
  }

  function updateRangeMin(
    value: string,
    maxValue: string,
    setMin: (nextValue: string) => void,
    setMax: (nextValue: string) => void,
  ) {
    setMin(value)

    if (value !== '' && maxValue !== '' && Number(value) > Number(maxValue)) {
      setMax(value)
    }
  }

  function updateRangeMax(
    value: string,
    minValue: string,
    setMin: (nextValue: string) => void,
    setMax: (nextValue: string) => void,
  ) {
    setMax(value)

    if (value !== '' && minValue !== '' && Number(value) < Number(minValue)) {
      setMin(value)
    }
  }

  return (
    <header className={styles.header}>
      <strong className={styles.logo}>RoomHub</strong>

      <div className={styles.searchArea} ref={searchAreaRef}>
        <form
          className={styles.search}
          onSubmit={(event) => {
            event.preventDefault()
            runSearch()
          }}
        >
          <input
            className={styles.searchInput}
            type="search"
            placeholder="Поиск помещений..."
            aria-label="Поиск помещений"
            value={draftSearch}
            onChange={(event) => setDraftSearch(event.target.value)}
            onFocus={() => setFiltersOpen(true)}
          />
        </form>

        {filtersOpen && (
          <div className={styles.filters} aria-label="Фильтры каталога">
            <label className={styles.filterField}>
              <span>Город</span>
              <select value={draftCity} onChange={(event) => setDraftCity(event.target.value)}>
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
                  min={draftMinCapacity || '1'}
                  inputMode="numeric"
                  placeholder="Любая"
                  value={draftMaxCapacity}
                  onChange={(event) =>
                    updateRangeMax(
                      event.target.value,
                      draftMinCapacity,
                      setDraftMinCapacity,
                      setDraftMaxCapacity,
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
                  max={draftMaxCapacity || undefined}
                  inputMode="numeric"
                  placeholder="Любая"
                  value={draftMinCapacity}
                  onChange={(event) =>
                    updateRangeMin(
                      event.target.value,
                      draftMaxCapacity,
                      setDraftMinCapacity,
                      setDraftMaxCapacity,
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
                  min={draftMinPrice || '0'}
                  step="1000"
                  inputMode="numeric"
                  placeholder="Без лимита"
                  value={draftMaxPrice}
                  onChange={(event) =>
                    updateRangeMax(
                      event.target.value,
                      draftMinPrice,
                      setDraftMinPrice,
                      setDraftMaxPrice,
                    )
                  }
                  onWheel={(event) => event.currentTarget.blur()}
                />
              </label>
              <label>
                <span>от</span>
                <input
                  type="number"
                  min="0"
                  max={draftMaxPrice || undefined}
                  step="1000"
                  inputMode="numeric"
                  placeholder="Любая"
                  value={draftMinPrice}
                  onChange={(event) =>
                    updateRangeMin(
                      event.target.value,
                      draftMaxPrice,
                      setDraftMinPrice,
                      setDraftMaxPrice,
                    )
                  }
                  onWheel={(event) => event.currentTarget.blur()}
                />
              </label>
            </div>

            <button type="button" className={styles.searchButton} onClick={runSearch}>
              Поиск
            </button>

            <button type="button" className={styles.resetButton} onClick={resetFilters}>
              Сбросить
            </button>
          </div>
        )}
      </div>

      <nav className={styles.nav}>
        <NavLink to="/" className={({ isActive }) => (isActive ? `${styles.link} ${styles.active}` : styles.link)}>
          Помещения
        </NavLink>

        <NavLink to="/profile" className={({ isActive }) => (isActive ? `${styles.link} ${styles.active}` : styles.link)}>
          Профиль
        </NavLink>

        <NavLink to="/login" className={({ isActive }) => (isActive ? `${styles.link} ${styles.active}` : styles.link)}>
          Войти
        </NavLink>
      </nav>
    </header>
  )
}
