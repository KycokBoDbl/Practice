import type { FormEvent } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import {
  getListings,
  parseListingPublicationApiError,
  publishListing,
} from '../../api/listings'
import { useAuth } from '../../auth/useAuth'
import type { Listing } from '../../types/listing'
import {
  getSpaceTypeLabel,
  SPACE_TYPE_LABELS,
  type KnownSpaceType,
} from '../../types/spaceType'
import {
  AMENITY_GROUPS,
  RUSSIAN_CITY_OPTIONS,
} from './listingPublicationOptions'
import styles from './ListingPublicationPage.module.css'

interface PublicationFormState {
  title: string
  spaceType: KnownSpaceType
  city: string
  address: string
  capacity: string
  pricePerHour: string
  description: string
  imageUrl: string
}

interface PublicationFormErrors {
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

const INITIAL_FORM_STATE: PublicationFormState = {
  title: '',
  spaceType: 'MEETING_ROOM',
  city: '',
  address: '',
  capacity: '',
  pricePerHour: '',
  description: '',
  imageUrl: '',
}

const SPACE_TYPE_OPTIONS = Object.entries(SPACE_TYPE_LABELS) as Array<
  [KnownSpaceType, string]
>
const FIELD_ORDER: Array<keyof PublicationFormErrors> = [
  'title',
  'spaceType',
  'city',
  'address',
  'capacity',
  'pricePerHour',
  'description',
  'imageUrl',
]
const FIELD_IDS: Record<keyof PublicationFormState, string> = {
  title: 'publication-title',
  spaceType: 'publication-space-type',
  city: 'publication-city',
  address: 'publication-address',
  capacity: 'publication-capacity',
  pricePerHour: 'publication-price',
  description: 'publication-description',
  imageUrl: 'publication-image-url',
}

function normalizeOptionalValue(value: string) {
  const trimmedValue = value.trim()
  return trimmedValue === '' ? null : trimmedValue
}

function isValidHttpUrl(value: string) {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

function getUniqueSortedValues(values: string[]) {
  return Array.from(
    new Set(values.map((value) => value.trim()).filter(Boolean)),
  ).sort((left, right) => left.localeCompare(right, 'ru'))
}

function buildPublicationDescription(description: string, amenities: string[]) {
  const normalizedDescription = normalizeOptionalValue(description)

  if (amenities.length === 0) {
    return normalizedDescription
  }

  const amenityLine = `Удобства: ${amenities.join(', ')}.`
  return normalizedDescription
    ? `${normalizedDescription}\n\n${amenityLine}`
    : amenityLine
}

function formatPrice(value: string) {
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

function validateForm(form: PublicationFormState): PublicationFormErrors {
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

function hasErrors(errors: PublicationFormErrors) {
  return Object.values(errors).some(Boolean)
}

export function ListingPublicationPage() {
  const { profile } = useAuth()
  const [form, setForm] = useState<PublicationFormState>(INITIAL_FORM_STATE)
  const [errors, setErrors] = useState<PublicationFormErrors>({})
  const [submitting, setSubmitting] = useState(false)
  const [createdListing, setCreatedListing] = useState<Listing | null>(null)
  const [catalogCities, setCatalogCities] = useState<string[]>([])
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([])

  useEffect(() => {
    let cancelled = false

    async function loadCatalogCities() {
      if (profile?.role !== 'LANDLORD') {
        return
      }

      try {
        const listings = await getListings()

        if (!cancelled) {
          setCatalogCities(
            getUniqueSortedValues(listings.map((listing) => listing.city)),
          )
        }
      } catch (error) {
        console.error('Не удалось загрузить города каталога:', error)
      }
    }

    loadCatalogCities()

    return () => {
      cancelled = true
    }
  }, [profile?.role])

  const cityOptions = useMemo(
    () => getUniqueSortedValues([...RUSSIAN_CITY_OPTIONS, ...catalogCities]),
    [catalogCities],
  )

  const pricePreview = formatPrice(form.pricePerHour)
  const imageUrl = form.imageUrl.trim()
  const canPreviewImage = imageUrl !== '' && isValidHttpUrl(imageUrl)

  function scrollToFirstError(nextErrors: PublicationFormErrors) {
    const firstInvalidField = FIELD_ORDER.find((field) => nextErrors[field])

    if (!firstInvalidField) {
      return
    }

    const fieldElement = document.getElementById(
      FIELD_IDS[firstInvalidField as keyof PublicationFormState],
    )

    if (!fieldElement) {
      return
    }

    fieldElement.scrollIntoView({
      behavior: 'smooth',
      block: 'center',
    })

    if ('focus' in fieldElement) {
      fieldElement.focus()
    }
  }

  function updateField<Field extends keyof PublicationFormState>(
    field: Field,
    value: PublicationFormState[Field],
  ) {
    setForm((currentForm) => ({
      ...currentForm,
      [field]: value,
    }))

    setErrors((currentErrors) => ({
      ...currentErrors,
      [field]: undefined,
      form: undefined,
    }))
  }

  function toggleAmenity(amenity: string) {
    setSelectedAmenities((currentAmenities) =>
      currentAmenities.includes(amenity)
        ? currentAmenities.filter((currentAmenity) => currentAmenity !== amenity)
        : [...currentAmenities, amenity],
    )
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setCreatedListing(null)

    const nextErrors = validateForm(form)

    if (hasErrors(nextErrors)) {
      setErrors(nextErrors)
      window.requestAnimationFrame(() => {
        scrollToFirstError(nextErrors)
      })
      return
    }

    setSubmitting(true)
    setErrors({})

    try {
      const listing = await publishListing({
        title: form.title.trim(),
        spaceType: form.spaceType,
        city: form.city.trim(),
        address: form.address.trim(),
        capacity: Number(form.capacity),
        pricePerHour: Number(form.pricePerHour),
        description: buildPublicationDescription(
          form.description,
          selectedAmenities,
        ),
        imageUrl: normalizeOptionalValue(form.imageUrl),
      })

      setCreatedListing(listing)
    } catch (error) {
      const parsedError = parseListingPublicationApiError(error)

      setErrors({
        title: parsedError.fieldErrors.title,
        spaceType: parsedError.fieldErrors.spaceType,
        city: parsedError.fieldErrors.city,
        address: parsedError.fieldErrors.address,
        capacity: parsedError.fieldErrors.capacity,
        pricePerHour: parsedError.fieldErrors.pricePerHour,
        description: parsedError.fieldErrors.description,
        imageUrl: parsedError.fieldErrors.imageUrl,
        form:
          parsedError.kind === 'forbidden'
            ? 'Публиковать объявления могут только аккаунты арендодателей.'
            : parsedError.message,
      })
    } finally {
      setSubmitting(false)
    }
  }

  if (profile?.role !== 'LANDLORD') {
    return (
      <main className={styles.page}>
        <section className={`${styles.panel} ${styles.restrictedPanel}`}>
          <p className={styles.eyebrow}>Публикация объявления</p>
          <h1>Недоступно для вашей роли</h1>
          <p className={styles.text}>
            Размещать помещения могут только аккаунты арендодателей.
          </p>
          <Link to="/" className={styles.linkButton}>
            К каталогу
          </Link>
        </section>
      </main>
    )
  }

  return (
    <main className={styles.page}>
      <section className={styles.header}>
        <p className={styles.eyebrow}>Публикация объявления</p>
        <h1>Новое помещение</h1>
        <p className={styles.text}>
          Заполните данные помещения, чтобы опубликовать его в каталоге.
        </p>
      </section>

      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <div className={styles.grid}>
          <label className={styles.field}>
            <span>Название</span>
            <input
              id={FIELD_IDS.title}
              type="text"
              value={form.title}
              onChange={(event) => updateField('title', event.target.value)}
              aria-invalid={errors.title ? 'true' : undefined}
              aria-describedby={errors.title ? 'publication-title-error' : undefined}
              autoComplete="off"
            />
            {errors.title && (
              <small id="publication-title-error" className={styles.error}>
                {errors.title}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>Тип помещения</span>
            <select
              id={FIELD_IDS.spaceType}
              value={form.spaceType}
              onChange={(event) =>
                updateField('spaceType', event.target.value as KnownSpaceType)
              }
              aria-invalid={errors.spaceType ? 'true' : undefined}
              aria-describedby={
                errors.spaceType ? 'publication-space-type-error' : undefined
              }
            >
              {SPACE_TYPE_OPTIONS.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
            {errors.spaceType && (
              <small id="publication-space-type-error" className={styles.error}>
                {errors.spaceType}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>Город</span>
            <input
              id={FIELD_IDS.city}
              type="text"
              list="publication-city-suggestions"
              value={form.city}
              onChange={(event) => updateField('city', event.target.value)}
              aria-invalid={errors.city ? 'true' : undefined}
              aria-describedby={errors.city ? 'publication-city-error' : undefined}
              autoComplete="address-level2"
            />
            <datalist id="publication-city-suggestions">
              {cityOptions.map((city) => (
                <option key={city} value={city} />
              ))}
            </datalist>
            {errors.city && (
              <small id="publication-city-error" className={styles.error}>
                {errors.city}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>Адрес</span>
            <input
              id={FIELD_IDS.address}
              type="text"
              value={form.address}
              onChange={(event) => updateField('address', event.target.value)}
              aria-invalid={errors.address ? 'true' : undefined}
              aria-describedby={
                errors.address ? 'publication-address-error' : undefined
              }
              autoComplete="street-address"
            />
            {errors.address && (
              <small id="publication-address-error" className={styles.error}>
                {errors.address}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>Вместимость</span>
            <input
              id={FIELD_IDS.capacity}
              type="number"
              min="1"
              step="1"
              value={form.capacity}
              onChange={(event) => updateField('capacity', event.target.value)}
              aria-invalid={errors.capacity ? 'true' : undefined}
              aria-describedby={
                errors.capacity ? 'publication-capacity-error' : undefined
              }
              inputMode="numeric"
            />
            {errors.capacity && (
              <small id="publication-capacity-error" className={styles.error}>
                {errors.capacity}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>Цена за час</span>
            <input
              id={FIELD_IDS.pricePerHour}
              type="number"
              min="100"
              step="100"
              value={form.pricePerHour}
              onChange={(event) => updateField('pricePerHour', event.target.value)}
              aria-invalid={errors.pricePerHour ? 'true' : undefined}
              aria-describedby={
                errors.pricePerHour ? 'publication-price-error' : undefined
              }
              inputMode="numeric"
            />
            <small className={styles.helper}>{pricePreview}</small>
            {errors.pricePerHour && (
              <small id="publication-price-error" className={styles.error}>
                {errors.pricePerHour}
              </small>
            )}
          </label>
        </div>

        <label className={styles.field}>
          <span>Описание</span>
          <textarea
            id={FIELD_IDS.description}
            value={form.description}
            onChange={(event) => updateField('description', event.target.value)}
            aria-invalid={errors.description ? 'true' : undefined}
            aria-describedby={
              errors.description ? 'publication-description-error' : undefined
            }
            rows={5}
          />
          {errors.description && (
            <small id="publication-description-error" className={styles.error}>
              {errors.description}
            </small>
          )}
        </label>

        <section className={styles.amenities}>
          <div>
            <h2>Удобства</h2>
            <p>{selectedAmenities.length ? selectedAmenities.join(', ') : 'Не выбраны'}</p>
          </div>

          <div className={styles.amenityGroups}>
            {AMENITY_GROUPS.map((group) => (
              <fieldset key={group.title} className={styles.amenityGroup}>
                <legend>{group.title}</legend>
                <div className={styles.chips}>
                  {group.options.map((amenity) => {
                    const selected = selectedAmenities.includes(amenity)

                    return (
                      <button
                        key={amenity}
                        type="button"
                        className={`${styles.chip} ${selected ? styles.chipSelected : ''}`}
                        onClick={() => toggleAmenity(amenity)}
                        aria-pressed={selected}
                      >
                        {amenity}
                      </button>
                    )
                  })}
                </div>
              </fieldset>
            ))}
          </div>
        </section>

        <label className={styles.field}>
          <span>Ссылка на изображение</span>
          <input
            id={FIELD_IDS.imageUrl}
            type="url"
            value={form.imageUrl}
            onChange={(event) => updateField('imageUrl', event.target.value)}
            aria-invalid={errors.imageUrl ? 'true' : undefined}
            aria-describedby={
              errors.imageUrl ? 'publication-image-url-error' : undefined
            }
            placeholder="https://example.com/image.jpg"
          />
          {errors.imageUrl && (
            <small id="publication-image-url-error" className={styles.error}>
              {errors.imageUrl}
            </small>
          )}
        </label>

        <div className={styles.imagePreview}>
          {canPreviewImage ? (
            <img src={imageUrl} alt={form.title || 'Изображение помещения'} />
          ) : (
            <div className={styles.imagePlaceholder}>
              Изображение не добавлено
            </div>
          )}
        </div>

        <section className={styles.summary}>
          <h2>Сводка</h2>
          <dl>
            <div>
              <dt>Тип</dt>
              <dd>{getSpaceTypeLabel(form.spaceType)}</dd>
            </div>
            <div>
              <dt>Город</dt>
              <dd>{form.city.trim() || 'Не указан'}</dd>
            </div>
            <div>
              <dt>Вместимость</dt>
              <dd>{form.capacity.trim() || 'Не указана'}</dd>
            </div>
            <div>
              <dt>Цена</dt>
              <dd>{pricePreview}</dd>
            </div>
            <div>
              <dt>Удобства</dt>
              <dd>{selectedAmenities.length || 'Не выбраны'}</dd>
            </div>
          </dl>
        </section>

        {errors.form && (
          <p className={styles.error} role="alert">
            {errors.form}
          </p>
        )}

        {createdListing && (
          <div className={styles.success} role="status">
            <p>Объявление опубликовано.</p>
            <Link to={`/spaces/${createdListing.id}`}>
              Открыть объявление
            </Link>
          </div>
        )}

        <div className={styles.actions}>
          <button type="submit" className={styles.submitButton} disabled={submitting}>
            {submitting ? 'Публикуем...' : 'Опубликовать'}
          </button>
        </div>
      </form>
    </main>
  )
}
