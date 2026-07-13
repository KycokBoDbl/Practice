import type { ListingLifecycleStatus, OwnedListing } from '../../types/listing'
import { SPACE_TYPE_LABELS, type KnownSpaceType } from '../../types/spaceType'

export type ManagementState = 'loading' | 'loaded' | 'empty' | 'error'
export type ListingFilter = 'all' | ListingLifecycleStatus
export type ListingAction = 'hide' | 'activate' | 'delete'

export interface EditFormState {
  title: string
  spaceType: KnownSpaceType
  city: string
  address: string
  capacity: string
  pricePerHour: string
  description: string
  imageUrl: string
}

export interface EditFormErrors {
  title?: string
  spaceType?: string
  city?: string
  address?: string
  capacity?: string
  pricePerHour?: string
  description?: string
  imageUrl?: string
  form?: string
}

const CURRENCY_FORMAT = new Intl.NumberFormat('ru-RU')
const AMENITY_LINE_PREFIX = 'Удобства: '

export const FILTERS: Array<{ value: ListingFilter; label: string }> = [
  { value: 'all', label: 'Все' },
  { value: 'PUBLISHED', label: 'Активные' },
  { value: 'ARCHIVED', label: 'Скрытые' },
]

export const STATUS_LABELS: Record<ListingLifecycleStatus, string> = {
  PUBLISHED: 'Активное',
  ARCHIVED: 'Скрытое',
}

export const STATE_TITLES: Record<ManagementState, string> = {
  loading: 'Загружаем ваши объявления...',
  loaded: 'Готово',
  empty: 'Объявлений пока нет',
  error: 'Не удалось загрузить объявления',
}

export const SPACE_TYPE_OPTIONS = Object.entries(SPACE_TYPE_LABELS) as Array<
  [KnownSpaceType, string]
>

export const EDIT_FIELD_IDS: Record<keyof EditFormState, string> = {
  title: 'listing-edit-title',
  spaceType: 'listing-edit-space-type',
  city: 'listing-edit-city',
  address: 'listing-edit-address',
  capacity: 'listing-edit-capacity',
  pricePerHour: 'listing-edit-price',
  description: 'listing-edit-description',
  imageUrl: 'listing-edit-image-url',
}

export const EDIT_FIELD_ORDER: Array<keyof EditFormState> = [
  'title',
  'spaceType',
  'city',
  'address',
  'capacity',
  'pricePerHour',
  'description',
  'imageUrl',
]

export function formatPrice(value: number) {
  return `${CURRENCY_FORMAT.format(value)} ₽`
}

export function normalizeText(value: string | null | undefined) {
  const text = value?.trim()
  return text ? text : 'Не указан'
}

export function normalizeOptionalValue(value: string) {
  const trimmedValue = value.trim()
  return trimmedValue === '' ? null : trimmedValue
}

export function isValidHttpUrl(value: string) {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

export function buildListingDescription(description: string, amenities: string[]) {
  const normalizedDescription = normalizeOptionalValue(description)

  if (amenities.length === 0) {
    return normalizedDescription
  }

  const amenityLine = `${AMENITY_LINE_PREFIX}${amenities.join(', ')}.`

  return normalizedDescription ? `${normalizedDescription}\n\n${amenityLine}` : amenityLine
}

export function splitListingDescription(value: string | null | undefined) {
  const trimmedValue = value?.trim()

  if (!trimmedValue) {
    return { description: '', amenities: [] as string[] }
  }

  const lines = trimmedValue.split(/\r?\n/)
  const lastLine = lines[lines.length - 1]?.trim()

  if (!lastLine?.startsWith(AMENITY_LINE_PREFIX) || !lastLine.endsWith('.')) {
    return { description: trimmedValue, amenities: [] as string[] }
  }

  const amenities = Array.from(
    new Set(
      lastLine
        .slice(AMENITY_LINE_PREFIX.length, -1)
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

export function validateEditForm(form: EditFormState): EditFormErrors {
  const errors: EditFormErrors = {}
  const capacity = Number(form.capacity)
  const pricePerHour = Number(form.pricePerHour)

  if (!form.title.trim()) {
    errors.title = 'Укажите название помещения.'
  }

  if (!form.city.trim()) {
    errors.city = 'Укажите город.'
  }

  if (!form.address.trim()) {
    errors.address = 'Укажите адрес.'
  }

  if (!Number.isInteger(capacity) || capacity < 1) {
    errors.capacity = 'Вместимость должна быть целым числом от 1.'
  }

  if (!Number.isFinite(pricePerHour) || pricePerHour <= 0) {
    errors.pricePerHour = 'Цена за час должна быть положительным числом.'
  }

  const imageUrl = form.imageUrl.trim()

  if (imageUrl && !isValidHttpUrl(imageUrl)) {
    errors.imageUrl = 'Ссылка на изображение должна начинаться с http:// или https://.'
  }

  return errors
}

export function hasErrors(errors: EditFormErrors) {
  return Object.values(errors).some(Boolean)
}

export function getInitialEditForm(listing: OwnedListing): EditFormState {
  const { description } = splitListingDescription(listing.description)

  return {
    title: listing.title,
    spaceType: listing.spaceType as KnownSpaceType,
    city: listing.city,
    address: listing.address,
    capacity: String(listing.capacity),
    pricePerHour: String(listing.pricePerHour),
    description,
    imageUrl: listing.imageUrl ?? '',
  }
}
