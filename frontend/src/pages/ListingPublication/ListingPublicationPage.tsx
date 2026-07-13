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
import { type KnownSpaceType } from '../../types/spaceType'
import { RUSSIAN_CITY_OPTIONS } from './listingPublicationOptions'
import { PublicationAmenityGroups } from './PublicationAmenityGroups'
import { PublicationPreview } from './PublicationPreview'
import {
  buildPublicationDescription,
  FIELD_IDS,
  FIELD_ORDER,
  formatPrice,
  getUniqueSortedValues,
  hasErrors,
  isValidHttpUrl,
  normalizeOptionalValue,
  SPACE_TYPE_OPTIONS,
  validateForm,
  type PublicationFormErrors,
} from './listingPublicationForm'
import styles from './ListingPublicationPage.module.css'
import { useListingPublicationForm } from './useListingPublicationForm'

export function ListingPublicationPage() {
  const { profile } = useAuth()
  const { errors, form, selectedAmenities, setErrors, toggleAmenity, updateField } =
    useListingPublicationForm()
  const [submitting, setSubmitting] = useState(false)
  const [createdListing, setCreatedListing] = useState<Listing | null>(null)
  const [catalogCities, setCatalogCities] = useState<string[]>([])

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
  const previewDescription = buildPublicationDescription(
    form.description,
    selectedAmenities,
  )

  function scrollToFirstError(nextErrors: PublicationFormErrors) {
    const firstInvalidField = FIELD_ORDER.find((field) => nextErrors[field])

    if (!firstInvalidField) {
      return
    }

    const fieldElement = document.getElementById(FIELD_IDS[firstInvalidField])

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
        description: previewDescription,
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
        <section className={styles.panel}>
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
        <div>
          <p className={styles.eyebrow}>Публикация объявления</p>
          <h1>Новое помещение</h1>
          <p className={styles.text}>
            Заполните данные слева, а справа сразу проверяйте, как объявление будет выглядеть в
            каталоге.
          </p>
        </div>

        <Link to="/my-listings" className={styles.ghostLink}>
          К моим объявлениям
        </Link>
      </section>

      <form className={styles.layout} onSubmit={handleSubmit} noValidate>
        <div className={styles.formBody}>
          <section className={styles.editorColumn}>
          <article className={styles.editorSection}>
            <div className={styles.sectionHeader}>
              <h2>Основное</h2>
              <p>Название, тип помещения, город, адрес, вместимость и стоимость.</p>
            </div>

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
                <small className={styles.helperPlaceholder} aria-hidden="true">
                  &nbsp;
                </small>
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
          </article>

          <article className={styles.editorSection}>
            <div className={styles.sectionHeader}>
              <h2>Описание</h2>
              <p>Текст карточки и дополнительные детали, которые увидит арендатор.</p>
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
                rows={6}
              />
              {errors.description && (
                <small id="publication-description-error" className={styles.error}>
                  {errors.description}
                </small>
              )}
            </label>
          </article>

          <article className={styles.editorSection}>
            <div className={styles.sectionHeader}>
              <h2>Удобства</h2>
              <p>
                Выбранные теги автоматически попадут в описание и будут видны в живом превью.
              </p>
            </div>

            <PublicationAmenityGroups
              selectedAmenities={selectedAmenities}
              onToggleAmenity={toggleAmenity}
            />
          </article>

          <article className={styles.editorSection}>
            <div className={styles.sectionHeader}>
              <h2>Медиа</h2>
              <p>Укажите ссылку на изображение, если оно уже загружено во внешнее хранилище.</p>
            </div>

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
          </article>

          </section>

          <PublicationPreview
            canPreviewImage={canPreviewImage}
            createdListing={createdListing}
            error={errors.form}
            form={form}
            imageUrl={imageUrl}
            ownerName={profile.legalName}
            previewDescription={previewDescription}
            pricePreview={pricePreview}
            selectedAmenities={selectedAmenities}
          />
        </div>

        <div className={styles.formActions}>
          <button type="submit" className={styles.submitButton} disabled={submitting}>
            {submitting ? 'Публикуем...' : 'Опубликовать'}
          </button>
        </div>
      </form>
    </main>
  )
}
