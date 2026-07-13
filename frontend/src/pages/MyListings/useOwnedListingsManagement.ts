import { useCallback, useEffect, useState } from 'react'

import {
  activateListing,
  deleteListing,
  getOwnedListings,
  hideListing,
  parseListingManagementApiError,
} from '../../api/listings'
import type { ListingLifecycleStatus, OwnedListing } from '../../types/listing'
import type { ListingAction, ManagementState } from './myListingsHelpers'

export type ListingNotice = { kind: 'error' | 'success'; text: string } | null

export function useOwnedListingsManagement(enabled: boolean) {
  const [items, setItems] = useState<OwnedListing[]>([])
  const [state, setState] = useState<ManagementState>('loading')
  const [message, setMessage] = useState('')
  const [notice, setNotice] = useState<ListingNotice>(null)
  const [pendingAction, setPendingAction] = useState<{
    listingId: number
    action: ListingAction
  } | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadOwnedListings() {
      if (!enabled) {
        return
      }

      setState('loading')
      setMessage('')

      try {
        const ownedListings = await getOwnedListings()

        if (cancelled) {
          return
        }

        setItems(ownedListings)
        setState(ownedListings.length === 0 ? 'empty' : 'loaded')
      } catch (error) {
        if (cancelled) {
          return
        }

        const parsedError = parseListingManagementApiError(error)
        setItems([])
        setState('error')
        setMessage(parsedError.message || 'Не удалось загрузить список объявлений.')
      }
    }

    void loadOwnedListings()

    return () => {
      cancelled = true
    }
  }, [enabled])

  const updateListingStatus = useCallback(
    (listingId: number, status: ListingLifecycleStatus) => {
      setItems((currentItems) =>
        currentItems.map((item) => (item.id === listingId ? { ...item, status } : item)),
      )
    },
    [],
  )

  const removeListing = useCallback((listingId: number) => {
    setItems((currentItems) => currentItems.filter((item) => item.id !== listingId))
  }, [])

  const handleListingAction = useCallback(
    async (listing: OwnedListing, action: ListingAction) => {
      setNotice(null)

      if (action === 'delete') {
        const confirmed = window.confirm('Удалить объявление безвозвратно?')

        if (!confirmed) {
          return
        }
      }

      setPendingAction({ listingId: listing.id, action })

      try {
        if (action === 'hide') {
          await hideListing(listing.id)
          updateListingStatus(listing.id, 'ARCHIVED')
          setNotice({ kind: 'success', text: 'Объявление скрыто.' })
          return
        }

        if (action === 'activate') {
          await activateListing(listing.id)
          updateListingStatus(listing.id, 'PUBLISHED')
          setNotice({ kind: 'success', text: 'Объявление снова опубликовано.' })
          return
        }

        await deleteListing(listing.id)
        removeListing(listing.id)
        setNotice({ kind: 'success', text: 'Объявление удалено.' })
      } catch (error) {
        const parsedError = parseListingManagementApiError(error)

        setNotice({
          kind: 'error',
          text:
            action === 'delete' && parsedError.kind === 'conflict'
              ? 'Нельзя удалить объявление, пока по нему сохраняется история бронирований.'
              : parsedError.kind === 'forbidden'
                ? 'Для этого действия недостаточно прав.'
                : parsedError.kind === 'notFound'
                  ? 'Объявление не найдено.'
                  : parsedError.message || 'Не удалось выполнить действие.',
        })
      } finally {
        setPendingAction(null)
      }
    },
    [removeListing, updateListingStatus],
  )

  return {
    handleListingAction,
    items,
    message,
    notice,
    pendingAction,
    setItems,
    setNotice,
    state,
  }
}
