import { SPACE_TYPE_LABELS, type KnownSpaceType } from '../../types/spaceType'

export interface PublicationFormState {
  title: string
  spaceType: KnownSpaceType
  city: string
  address: string
  capacity: string
  pricePerHour: string
  description: string
  imageUrl: string
}

export interface PublicationFormErrors {
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

export const INITIAL_FORM_STATE: PublicationFormState = {
  title: '',
  spaceType: 'MEETING_ROOM',
  city: '',
  address: '',
  capacity: '',
  pricePerHour: '',
  description: '',
  imageUrl: '',
}

export const SPACE_TYPE_OPTIONS = Object.entries(SPACE_TYPE_LABELS) as Array<
  [KnownSpaceType, string]
>

export const FIELD_ORDER: Array<keyof PublicationFormState> = [
  'title',
  'spaceType',
  'city',
  'address',
  'capacity',
  'pricePerHour',
  'description',
  'imageUrl',
]

export const FIELD_IDS: Record<keyof PublicationFormState, string> = {
  title: 'publication-title',
  spaceType: 'publication-space-type',
  city: 'publication-city',
  address: 'publication-address',
  capacity: 'publication-capacity',
  pricePerHour: 'publication-price',
  description: 'publication-description',
  imageUrl: 'publication-image-url',
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

export function getUniqueSortedValues(values: string[]) {
  return Array.from(
    new Set(values.map((value) => value.trim()).filter(Boolean)),
  ).sort((left, right) => left.localeCompare(right, 'ru'))
}

export function buildPublicationDescription(description: string, amenities: string[]) {
  const normalizedDescription = normalizeOptionalValue(description)

  if (amenities.length === 0) {
    return normalizedDescription
  }

  const amenityLine = `Удобства: ${amenities.join(', ')}.`
  return normalizedDescription
    ? `${normalizedDescription}\n\n${amenityLine}`
    : amenityLine
}

export function formatPrice(value: string) {
  const price = Number(value)

  if (!Number.isFinite(price) || price <= 0) {
    return 'Не указана'
  }

  return `${new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: price % 1 === 0 ? 0 : 2,
  }).format(price)}/час`
}

export function validateForm(form: PublicationFormState): PublicationFormErrors {
  const errors: PublicationFormErrors = {}
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

export function hasErrors(errors: PublicationFormErrors) {
  return Object.values(errors).some(Boolean)
}
