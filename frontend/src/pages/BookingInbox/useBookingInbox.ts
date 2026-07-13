import { useCallback, useEffect, useRef, useState } from 'react'

import { getBookingInbox, parseBookingApiError } from '../../api/bookings'
import type { BookingInboxItem } from '../../types/booking'

export type InboxState = 'loading' | 'loaded' | 'empty' | 'error'

const POLL_INTERVAL_MS = 30000
const LOAD_ERROR_MESSAGE = 'Не удалось загрузить список заявок.'
const REFRESH_ERROR_MESSAGE = 'Не удалось обновить список заявок.'

type LoadMode = 'initial' | 'refresh' | 'poll'

export function useBookingInbox(reloadKey: string) {
  const [items, setItems] = useState<BookingInboxItem[]>([])
  const [state, setState] = useState<InboxState>('loading')
  const [message, setMessage] = useState('')
  const [refreshing, setRefreshing] = useState(false)
  const [requestPending, setRequestPending] = useState(false)
  const generationRef = useRef(0)
  const mountedRef = useRef(false)
  const requestInFlightGenerationRef = useRef<number | null>(null)

  const loadInbox = useCallback(async (mode: LoadMode, generation: number) => {
    if (requestInFlightGenerationRef.current === generation) {
      return
    }

    requestInFlightGenerationRef.current = generation
    setRequestPending(true)

    if (mode === 'initial') {
      setState('loading')
      setMessage('')
    }

    if (mode === 'refresh') {
      setRefreshing(true)
    }

    try {
      const inbox = await getBookingInbox()

      if (!mountedRef.current || generationRef.current !== generation) {
        return
      }

      setItems(inbox)
      setState(inbox.length === 0 ? 'empty' : 'loaded')
      setMessage('')
    } catch (error) {
      if (!mountedRef.current || generationRef.current !== generation) {
        return
      }

      const parsedError = parseBookingApiError(error)
      setMessage(
        parsedError.message ||
          (mode === 'initial' ? LOAD_ERROR_MESSAGE : REFRESH_ERROR_MESSAGE),
      )

      if (mode === 'initial') {
        setItems([])
        setState('error')
      }
    } finally {
      if (requestInFlightGenerationRef.current === generation) {
        requestInFlightGenerationRef.current = null
      }

      if (mountedRef.current && generationRef.current === generation) {
        setRefreshing(false)
        setRequestPending(false)
      }
    }
  }, [])

  const refresh = useCallback(() => {
    void loadInbox('refresh', generationRef.current)
  }, [loadInbox])

  useEffect(() => {
    mountedRef.current = true
    generationRef.current += 1

    const generation = generationRef.current
    void loadInbox('initial', generation)

    const pollIntervalId = window.setInterval(() => {
      void loadInbox('poll', generation)
    }, POLL_INTERVAL_MS)

    return () => {
      window.clearInterval(pollIntervalId)
      mountedRef.current = false
    }
  }, [loadInbox, reloadKey])

  return {
    items,
    message,
    refresh,
    refreshing,
    requestPending,
    state,
  }
}
