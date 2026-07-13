import type { Listing } from '../../types/listing'
import { getSpaceTypeLabel } from '../../types/spaceType'

export const catalogSearchParamNames = [
  'q',
  'city',
  'minCapacity',
  'maxCapacity',
  'minPrice',
  'maxPrice',
] as const

// Preserve the existing committed catalog state in URL query params.
export type CatalogSearchParamName = (typeof catalogSearchParamNames)[number]

export interface CatalogQuery {
  q: string
  city: string
  minCapacity: string
  maxCapacity: string
  minPrice: string
  maxPrice: string
}

export const emptyCatalogQuery: CatalogQuery = {
  q: '',
  city: '',
  minCapacity: '',
  maxCapacity: '',
  minPrice: '',
  maxPrice: '',
}

export interface CatalogFilterBadge {
  key: CatalogSearchParamName
  label: string
  value: string
}

export function parseCatalogQuery(searchParams: URLSearchParams): CatalogQuery {
  return {
    q: searchParams.get('q') ?? '',
    city: searchParams.get('city') ?? '',
    minCapacity: searchParams.get('minCapacity') ?? '',
    maxCapacity: searchParams.get('maxCapacity') ?? '',
    minPrice: searchParams.get('minPrice') ?? '',
    maxPrice: searchParams.get('maxPrice') ?? '',
  }
}

export function setCatalogQueryParams(
  searchParams: URLSearchParams,
  query: CatalogQuery,
) {
  const nextParams = new URLSearchParams(searchParams)

  nextParams.delete('filters')
  setParam(nextParams, 'q', query.q)
  setParam(nextParams, 'city', query.city)
  setParam(nextParams, 'minCapacity', query.minCapacity)
  setParam(nextParams, 'maxCapacity', query.maxCapacity)
  setParam(nextParams, 'minPrice', query.minPrice)
  setParam(nextParams, 'maxPrice', query.maxPrice)

  return nextParams
}

export function getCatalogCityOptions(listings: Listing[]) {
  return Array.from(new Set(listings.map((listing) => listing.city)))
    .filter(Boolean)
    .sort((left, right) => left.localeCompare(right, 'ru-RU'))
}

export function isCatalogQueryEmpty(query: CatalogQuery) {
  return catalogSearchParamNames.every((name) => query[name].trim() === '')
}

export function getActiveCatalogFilterBadges(query: CatalogQuery): CatalogFilterBadge[] {
  const badges: CatalogFilterBadge[] = []

  if (query.q.trim()) {
    badges.push({
      key: 'q',
      label: 'Поиск',
      value: query.q.trim(),
    })
  }

  if (query.city.trim()) {
    badges.push({
      key: 'city',
      label: 'Город',
      value: query.city.trim(),
    })
  }

  if (query.minCapacity.trim()) {
    badges.push({
      key: 'minCapacity',
      label: 'Вместимость от',
      value: query.minCapacity.trim(),
    })
  }

  if (query.maxCapacity.trim()) {
    badges.push({
      key: 'maxCapacity',
      label: 'Вместимость до',
      value: query.maxCapacity.trim(),
    })
  }

  if (query.minPrice.trim()) {
    badges.push({
      key: 'minPrice',
      label: 'Цена от',
      value: `${query.minPrice.trim()} ₽/час`,
    })
  }

  if (query.maxPrice.trim()) {
    badges.push({
      key: 'maxPrice',
      label: 'Цена до',
      value: `${query.maxPrice.trim()} ₽/час`,
    })
  }

  return badges
}

export function filterCatalogListings(
  listings: Listing[],
  query: CatalogQuery,
) {
  const searchQuery = query.q.trim().toLowerCase()
  const searchTokens = searchQuery
    .split(/\s+/)
    .filter((token) => token.length >= 3)
  const minCapacityValue = Number(query.minCapacity)
  const maxCapacityValue = Number(query.maxCapacity)
  const minPriceValue = Number(query.minPrice)
  const maxPriceValue = Number(query.maxPrice)

  return listings.filter((listing) => {
    const searchText = [
      listing.title,
      listing.description,
      listing.city,
      listing.address,
      listing.spaceType,
      getSpaceTypeLabel(listing.spaceType),
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    const matchesSearch =
      searchTokens.length === 0 ||
      searchTokens.every((token) => searchText.includes(token))
    const matchesCity = query.city === '' || listing.city === query.city
    const matchesMinCapacity =
      query.minCapacity === '' || listing.capacity >= minCapacityValue
    const matchesMaxCapacity =
      query.maxCapacity === '' || listing.capacity <= maxCapacityValue
    const matchesMinPrice =
      query.minPrice === '' || listing.pricePerHour >= minPriceValue
    const matchesMaxPrice =
      query.maxPrice === '' || listing.pricePerHour <= maxPriceValue

    return (
      matchesSearch &&
      matchesCity &&
      matchesMinCapacity &&
      matchesMaxCapacity &&
      matchesMinPrice &&
      matchesMaxPrice
    )
  })
}

function setParam(params: URLSearchParams, name: CatalogSearchParamName, value: string) {
  const nextValue = value.trim()

  if (nextValue) {
    params.set(name, nextValue)
  } else {
    params.delete(name)
  }
}
