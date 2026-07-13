import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { parseListingManagementApiError, updateListing } from '../../api/listings'
import { useAuth } from '../../auth/useAuth'
import type { OwnedListing } from '../../types/listing'
import { getSpaceTypeLabel, type KnownSpaceType } from '../../types/spaceType'
import {
  AMENITY_GROUPS,
  RUSSIAN_CITY_OPTIONS,
} from '../ListingPublication/listingPublicationOptions'
import { MyListingCard } from './MyListingCard'
import {
  buildListingDescription,
  EDIT_FIELD_IDS,
  EDIT_FIELD_ORDER,
  FILTERS,
  formatPrice,
  getInitialEditForm,
  hasErrors,
  isValidHttpUrl,
  normalizeOptionalValue,
  normalizeText,
  SPACE_TYPE_OPTIONS,
  splitListingDescription,
  STATE_TITLES,
  STATUS_LABELS,
  validateEditForm,
  type EditFormErrors,
  type EditFormState,
  type ListingFilter,
} from './myListingsHelpers'
import styles from './MyListingsPage.module.css'
import { useOwnedListingsManagement } from './useOwnedListingsManagement'

export function MyListingsPage() {
  const { loading, profile } = useAuth()
  const {
    handleListingAction,
    items,
    message,
    notice,
    pendingAction,
    setItems,
    setNotice,
    state,
  } = useOwnedListingsManagement(!loading && profile?.role === 'LANDLORD')
  const [filter, setFilter] = useState<ListingFilter>('all')
  const [editingListing, setEditingListing] = useState<OwnedListing | null>(null)
  const [editForm, setEditForm] = useState<EditFormState | null>(null)
  const [editAmenities, setEditAmenities] = useState<string[]>([])
  const [editErrors, setEditErrors] = useState<EditFormErrors>({})
  const [saving, setSaving] = useState(false)

  const filteredItems = useMemo(() => {
    if (filter === 'all') {
      return items
    }

    return items.filter((item) => item.status === filter)
  }, [filter, items])

  const activeCount = items.filter((item) => item.status === 'PUBLISHED').length
  const hiddenCount = items.filter((item) => item.status === 'ARCHIVED').length

  function openEditForm(listing: OwnedListing) {
    setEditingListing(listing)
    setEditForm(getInitialEditForm(listing))
    setEditAmenities(splitListingDescription(listing.description).amenities)
    setEditErrors({})
    setNotice(null)
  }

  function closeEditForm() {
    if (saving) {
      return
    }

    setEditingListing(null)
    setEditForm(null)
    setEditAmenities([])
    setEditErrors({})
  }

  function updateEditField<Field extends keyof EditFormState>(
    field: Field,
    value: EditFormState[Field],
  ) {
    setEditForm((currentForm) => {
      if (!currentForm) {
        return currentForm
      }

      return {
        ...currentForm,
        [field]: value,
      }
    })

    setEditErrors((currentErrors) => ({
      ...currentErrors,
      [field]: undefined,
      form: undefined,
    }))
  }

  function toggleEditAmenity(amenity: string) {
    setEditAmenities((currentAmenities) =>
      currentAmenities.includes(amenity)
        ? currentAmenities.filter((currentAmenity) => currentAmenity !== amenity)
        : [...currentAmenities, amenity],
    )
  }

  function scrollToFirstError(nextErrors: EditFormErrors) {
    const firstInvalidField = EDIT_FIELD_ORDER.find((field) => nextErrors[field])

    if (!firstInvalidField) {
      return
    }

    const fieldElement = document.getElementById(EDIT_FIELD_IDS[firstInvalidField])

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

  async function handleEditSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!editingListing || !editForm) {
      return
    }

    const nextErrors = validateEditForm(editForm)

    if (hasErrors(nextErrors)) {
      setEditErrors(nextErrors)
      window.requestAnimationFrame(() => {
        scrollToFirstError(nextErrors)
      })
      return
    }

    setSaving(true)
    setEditErrors({})

    try {
      const description = buildListingDescription(editForm.description, editAmenities)
      const updatedListing = await updateListing(editingListing.id, {
        title: editForm.title.trim(),
        description,
        city: editForm.city.trim(),
        address: editForm.address.trim(),
        capacity: Number(editForm.capacity),
        pricePerHour: Number(editForm.pricePerHour),
        spaceType: editForm.spaceType,
        imageUrl: normalizeOptionalValue(editForm.imageUrl),
      })

      setItems((currentItems) =>
        currentItems.map((item) =>
          item.id === editingListing.id
            ? {
                ...item,
                ...updatedListing,
                status: item.status,
                ownerOrganizationName:
                  updatedListing.ownerOrganizationName ?? item.ownerOrganizationName,
              }
            : item,
        ),
      )
      setEditingListing(null)
      setEditForm(null)
      setEditAmenities([])
      setEditErrors({})
      setNotice({
        kind: 'success',
        text: 'Изменения сохранены.',
      })
    } catch (error) {
      const parsedError = parseListingManagementApiError(error)

      setEditErrors({
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
            ? 'Редактировать объявления может только владелец.'
            : parsedError.kind === 'notFound'
              ? 'Объявление не найдено.'
              : parsedError.message,
      })
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <main className={styles.page}>{STATE_TITLES.loading}</main>
  }

  if (profile?.role !== 'LANDLORD') {
    return (
      <main className={styles.page}>
        <section className={styles.shell}>
          <p className={styles.eyebrow}>Мои объявления</p>
          <h1>Раздел недоступен</h1>
          <p className={styles.text}>
            Управление объявлениями доступно только арендодателям с подтвержденной учетной записью.
          </p>
          <div className={styles.actions}>
            <Link to="/" className={styles.linkButton}>
              К каталогу
            </Link>
          </div>
        </section>
      </main>
    )
  }

  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Мои объявления</p>
            <h1>Управление объявлениями</h1>
            <p className={styles.text}>
              Здесь собраны ваши активные и скрытые объявления. Карточки уже подготовлены для
              последующих действий редактирования и публикации.
            </p>
          </div>

          <div className={styles.actions}>
            <Link to="/spaces/new" className={styles.primaryButton}>
              Опубликовать объявление
            </Link>
            <Link to="/" className={styles.linkButton}>
              К каталогу
            </Link>
          </div>
        </div>

        {state === 'error' && (
          <div className={styles.stateBox}>
            <h2 className={styles.stateTitle}>{STATE_TITLES.error}</h2>
            <p className={styles.stateText}>{message}</p>
          </div>
        )}

        {notice && (
          <div
            className={`${styles.stateBox} ${notice.kind === 'error' ? styles.noticeError : styles.noticeSuccess}`}
          >
            <p className={styles.stateText}>{notice.text}</p>
          </div>
        )}

        {state === 'empty' && (
          <div className={styles.stateBox}>
            <h2 className={styles.stateTitle}>{STATE_TITLES.empty}</h2>
            <p className={styles.stateText}>
              После публикации объявления они появятся здесь, а скрытые объявления будут доступны
              во вкладке со скрытыми.
            </p>
          </div>
        )}

        {state === 'loaded' && (
          <>
            <div className={styles.summary} aria-label="Сводка по объявлениям">
              <span>Всего: {items.length}</span>
              <span>Активные: {activeCount}</span>
              <span>Скрытые: {hiddenCount}</span>
            </div>

            <div className={styles.segmentedControl} role="tablist" aria-label="Фильтр объявлений">
              {FILTERS.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  className={`${styles.segmentButton} ${filter === item.value ? styles.segmentButtonActive : ''}`}
                  onClick={() => setFilter(item.value)}
                  aria-pressed={filter === item.value}
                >
                  {item.label}
                </button>
              ))}
            </div>

            {filteredItems.length === 0 ? (
              <div className={styles.stateBox}>
                <h2 className={styles.stateTitle}>В этом разделе пока нет объявлений</h2>
                <p className={styles.stateText}>
                  Переключите вкладку или создайте новое объявление, чтобы увидеть его в списке.
                </p>
              </div>
            ) : (
              <section className={styles.grid} aria-label="Список объявлений">
                {filteredItems.map((listing) => (
                  <MyListingCard
                    key={listing.id}
                    listing={listing}
                    pendingAction={pendingAction}
                    onAction={(targetListing, action) => {
                      void handleListingAction(targetListing, action)
                    }}
                    onEdit={openEditForm}
                  />
                ))}
              </section>
            )}
          </>
        )}
      </section>

      {editingListing && editForm && (
        <div className={styles.modalBackdrop} role="presentation" onClick={closeEditForm}>
          <section
            className={styles.modal}
            role="dialog"
            aria-modal="true"
            aria-labelledby="listing-edit-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className={styles.modalHeader}>
              <div>
                <p className={styles.eyebrow}>Редактирование</p>
                <h2 id="listing-edit-title">Объявление: {editingListing.title}</h2>
              </div>
              <button type="button" className={styles.modalClose} onClick={closeEditForm}>
                Закрыть
              </button>
            </div>

            <form className={styles.modalForm} onSubmit={handleEditSubmit} noValidate>
              <div className={styles.modalLayout}>
                <section className={styles.modalEditor}>
                  <article className={styles.modalSection}>
                    <div className={styles.sectionHeader}>
                      <h3>Основное</h3>
                      <p>Название, тип, город, адрес, вместимость и цена.</p>
                    </div>

                    <div className={styles.grid}>
                      <label className={styles.field}>
                        <span>Название</span>
                        <input
                          id={EDIT_FIELD_IDS.title}
                          type="text"
                          value={editForm.title}
                          onChange={(event) => updateEditField('title', event.target.value)}
                          aria-invalid={editErrors.title ? 'true' : undefined}
                          aria-describedby={editErrors.title ? 'listing-edit-title-error' : undefined}
                          autoComplete="off"
                        />
                        {editErrors.title && (
                          <small id="listing-edit-title-error" className={styles.error}>
                            {editErrors.title}
                          </small>
                        )}
                      </label>

                      <label className={styles.field}>
                        <span>Тип помещения</span>
                        <select
                          id={EDIT_FIELD_IDS.spaceType}
                          value={editForm.spaceType}
                          onChange={(event) =>
                            updateEditField('spaceType', event.target.value as KnownSpaceType)
                          }
                          aria-invalid={editErrors.spaceType ? 'true' : undefined}
                          aria-describedby={
                            editErrors.spaceType ? 'listing-edit-space-type-error' : undefined
                          }
                        >
                          {SPACE_TYPE_OPTIONS.map(([value, label]) => (
                            <option key={value} value={value}>
                              {label}
                            </option>
                          ))}
                        </select>
                        {editErrors.spaceType && (
                          <small id="listing-edit-space-type-error" className={styles.error}>
                            {editErrors.spaceType}
                          </small>
                        )}
                      </label>

                      <label className={styles.field}>
                        <span>Город</span>
                        <input
                          id={EDIT_FIELD_IDS.city}
                          type="text"
                          list="listing-edit-city-suggestions"
                          value={editForm.city}
                          onChange={(event) => updateEditField('city', event.target.value)}
                          aria-invalid={editErrors.city ? 'true' : undefined}
                          aria-describedby={editErrors.city ? 'listing-edit-city-error' : undefined}
                          autoComplete="address-level2"
                        />
                        <datalist id="listing-edit-city-suggestions">
                          {RUSSIAN_CITY_OPTIONS.map((city) => (
                            <option key={city} value={city} />
                          ))}
                        </datalist>
                        {editErrors.city && (
                          <small id="listing-edit-city-error" className={styles.error}>
                            {editErrors.city}
                          </small>
                        )}
                      </label>

                      <label className={styles.field}>
                        <span>Адрес</span>
                        <input
                          id={EDIT_FIELD_IDS.address}
                          type="text"
                          value={editForm.address}
                          onChange={(event) => updateEditField('address', event.target.value)}
                          aria-invalid={editErrors.address ? 'true' : undefined}
                          aria-describedby={
                            editErrors.address ? 'listing-edit-address-error' : undefined
                          }
                          autoComplete="street-address"
                        />
                        {editErrors.address && (
                          <small id="listing-edit-address-error" className={styles.error}>
                            {editErrors.address}
                          </small>
                        )}
                      </label>

                      <label className={styles.field}>
                        <span>Вместимость</span>
                        <input
                          id={EDIT_FIELD_IDS.capacity}
                          type="number"
                          min="1"
                          step="1"
                          value={editForm.capacity}
                          onChange={(event) => updateEditField('capacity', event.target.value)}
                          aria-invalid={editErrors.capacity ? 'true' : undefined}
                          aria-describedby={
                            editErrors.capacity ? 'listing-edit-capacity-error' : undefined
                          }
                          inputMode="numeric"
                        />
                        {editErrors.capacity && (
                          <small id="listing-edit-capacity-error" className={styles.error}>
                            {editErrors.capacity}
                          </small>
                        )}
                      </label>

                      <label className={styles.field}>
                        <span>Цена за час</span>
                        <input
                          id={EDIT_FIELD_IDS.pricePerHour}
                          type="number"
                          min="100"
                          step="100"
                          value={editForm.pricePerHour}
                          onChange={(event) => updateEditField('pricePerHour', event.target.value)}
                          aria-invalid={editErrors.pricePerHour ? 'true' : undefined}
                          aria-describedby={
                            editErrors.pricePerHour ? 'listing-edit-price-error' : undefined
                          }
                          inputMode="numeric"
                        />
                        {editErrors.pricePerHour && (
                          <small id="listing-edit-price-error" className={styles.error}>
                            {editErrors.pricePerHour}
                          </small>
                        )}
                      </label>
                    </div>
                  </article>

                  <article className={styles.modalSection}>
                    <div className={styles.sectionHeader}>
                      <h3>Описание и удобства</h3>
                      <p>Текст описания и отдельные теги для удобств, как на странице создания.</p>
                    </div>

                    <label className={styles.field}>
                      <span>Описание</span>
                      <textarea
                        id={EDIT_FIELD_IDS.description}
                        value={editForm.description}
                        onChange={(event) => updateEditField('description', event.target.value)}
                        aria-invalid={editErrors.description ? 'true' : undefined}
                        aria-describedby={
                          editErrors.description ? 'listing-edit-description-error' : undefined
                        }
                        rows={6}
                      />
                      {editErrors.description && (
                        <small id="listing-edit-description-error" className={styles.error}>
                          {editErrors.description}
                        </small>
                      )}
                    </label>

                    <div className={styles.amenityGroups}>
                      {AMENITY_GROUPS.map((group) => (
                        <fieldset key={group.title} className={styles.amenityGroup}>
                          <legend>{group.title}</legend>
                          <div className={styles.chips}>
                            {group.options.map((amenity) => {
                              const selected = editAmenities.includes(amenity)

                              return (
                                <button
                                  key={amenity}
                                  type="button"
                                  className={`${styles.chip} ${selected ? styles.chipSelected : ''}`}
                                  onClick={() => toggleEditAmenity(amenity)}
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
                  </article>

                  <article className={styles.modalSection}>
                    <div className={styles.sectionHeader}>
                      <h3>Медиа</h3>
                      <p>Ссылка на изображение, если карточка уже размещена на внешнем хостинге.</p>
                    </div>

                    <label className={styles.field}>
                      <span>Ссылка на изображение</span>
                      <input
                        id={EDIT_FIELD_IDS.imageUrl}
                        type="url"
                        value={editForm.imageUrl}
                        onChange={(event) => updateEditField('imageUrl', event.target.value)}
                        aria-invalid={editErrors.imageUrl ? 'true' : undefined}
                        aria-describedby={
                          editErrors.imageUrl ? 'listing-edit-image-url-error' : undefined
                        }
                        placeholder="https://example.com/image.jpg"
                      />
                      {editErrors.imageUrl && (
                        <small id="listing-edit-image-url-error" className={styles.error}>
                          {editErrors.imageUrl}
                        </small>
                      )}
                    </label>
                  </article>
                </section>

                <aside className={styles.previewColumn}>
                  <div className={styles.previewFrame}>
                    <div className={styles.previewImage}>
                      {editForm.imageUrl.trim() && isValidHttpUrl(editForm.imageUrl.trim()) ? (
                        <img
                          src={editForm.imageUrl.trim()}
                          alt={editForm.title || 'Изображение объявления'}
                        />
                      ) : (
                        <div className={styles.imagePlaceholder}>Изображение не добавлено</div>
                      )}
                    </div>

                    <div className={styles.previewBody}>
                      <div className={styles.previewTopline}>
                        <span className={styles.previewBadge}>Редактирование</span>
                        <span className={styles.previewBadgeMuted}>
                          {STATUS_LABELS[editingListing.status]}
                        </span>
                      </div>

                      <h2 className={styles.previewTitle}>
                        {editForm.title.trim() || 'Название объявления'}
                      </h2>

                      <p className={styles.previewMeta}>
                        {editForm.city.trim() || 'Город'} • {editForm.address.trim() || 'Адрес'}
                      </p>

                      <div className={styles.previewPrice}>
                        {formatPrice(Number(editForm.pricePerHour))} / час
                      </div>

                      <dl className={styles.previewFacts}>
                        <div>
                          <dt>Владелец</dt>
                          <dd>{normalizeText(editingListing.ownerOrganizationName)}</dd>
                        </div>
                        <div>
                          <dt>Тип</dt>
                          <dd>{getSpaceTypeLabel(editForm.spaceType)}</dd>
                        </div>
                        <div>
                          <dt>Вместимость</dt>
                          <dd>
                            {editForm.capacity.trim()
                              ? `до ${editForm.capacity.trim()} человек`
                              : 'Не указана'}
                          </dd>
                        </div>
                        <div>
                          <dt>Статус</dt>
                          <dd>{STATUS_LABELS[editingListing.status]}</dd>
                        </div>
                      </dl>

                      <section className={styles.previewSection}>
                        <h3>Описание</h3>
                        <p className={styles.previewDescription}>
                          {buildListingDescription(editForm.description, editAmenities) ||
                            'Описание и удобства появятся здесь после заполнения.'}
                        </p>
                      </section>

                      <section className={styles.previewSection}>
                        <h3>Удобства</h3>
                        <div className={styles.previewChips}>
                          {editAmenities.length > 0 ? (
                            editAmenities.map((amenity) => (
                              <span key={amenity} className={styles.previewChip}>
                                {amenity}
                              </span>
                            ))
                          ) : (
                            <span className={styles.previewHint}>Теги пока не выбраны</span>
                          )}
                        </div>
                      </section>
                    </div>
                  </div>
                </aside>
              </div>

              {editErrors.form && (
                <p className={styles.error} role="alert">
                  {editErrors.form}
                </p>
              )}

              <div className={styles.modalActions}>
                <button type="button" className={styles.linkButton} onClick={closeEditForm}>
                  Отмена
                </button>
                <button type="submit" className={styles.primaryButton} disabled={saving}>
                  {saving ? 'Сохраняем...' : 'Сохранить изменения'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </main>
  )
}

