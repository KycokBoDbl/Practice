import { useMemo, useState } from 'react'

import type { CreateBookingRequest } from '../../types/booking'
import {
  BookingSummary,
  DurationSection,
  MonthCard,
  TimeSlotsSection,
} from './BookingCalendarSections'
import styles from './BookingCalendar.module.css'
import { durations, timeSlots } from './constants'
import { useListingAvailability } from './useListingAvailability'
import {
  getDateTimeValue,
  getDaysOfMonth,
  getHourFromSlot,
  getNextBusyHour,
  getSlotStatus,
  toDateValue,
} from './utils'

export interface BookingCalendarPayload extends CreateBookingRequest {
  duration: number
}

interface BookingCalendarProps {
  listingId: number
  pricePerHour: number
  mode?: 'preview' | 'booking'
  bookingSubmitting?: boolean
  availabilityRefreshKey?: number
  onConfirmBooking?: (payload: BookingCalendarPayload) => void
}

export function BookingCalendar({
  listingId,
  pricePerHour,
  mode = 'booking',
  bookingSubmitting = false,
  availabilityRefreshKey = 0,
  onConfirmBooking,
}: BookingCalendarProps) {
  const today = new Date()

  const [visibleMonth, setVisibleMonth] = useState(
    () => new Date(today.getFullYear(), today.getMonth(), 1),
  )
  const currentMonth = new Date(today.getFullYear(), today.getMonth(), 1)
  const isCurrentMonth =
    visibleMonth.getFullYear() === currentMonth.getFullYear() &&
    visibleMonth.getMonth() === currentMonth.getMonth()

  const [selectedDate, setSelectedDate] = useState(toDateValue(today))
  const [selectedTime, setSelectedTime] = useState('09:00')
  const [duration, setDuration] = useState(2)
  const { availabilityError, availabilityLoading, busyIntervals } = useListingAvailability(
    listingId,
    visibleMonth,
    availabilityRefreshKey,
  )

  const calendarDays = useMemo(
    () => getDaysOfMonth(visibleMonth),
    [visibleMonth],
  )

  const firstAvailableSlot = timeSlots.find(
    (slot) => getSlotStatus(selectedDate, slot, busyIntervals) === 'available',
  )
  const selectedTimeStatus = getSlotStatus(selectedDate, selectedTime, busyIntervals)
  const resolvedSelectedTime =
    selectedTimeStatus === 'booked' && firstAvailableSlot
      ? firstAvailableSlot
      : selectedTime
  const selectedStartHour = getHourFromSlot(resolvedSelectedTime)
  const selectedSlotStatus = getSlotStatus(selectedDate, resolvedSelectedTime, busyIntervals)
  const nextBusyHour = getNextBusyHour(selectedDate, selectedStartHour, busyIntervals)
  const maxDuration =
    selectedSlotStatus === 'booked' ? 0 : Math.max(0, nextBusyHour - selectedStartHour)
  const availableDurations = durations.filter((item) => item <= maxDuration)
  const resolvedDuration =
    availableDurations.length === 0
      ? 0
      : duration === 0 || duration > maxDuration
        ? availableDurations[availableDurations.length - 1]
        : duration
  const totalPrice = pricePerHour * resolvedDuration
  const bookingPayload = useMemo<BookingCalendarPayload | null>(() => {
    if (resolvedDuration === 0 || selectedSlotStatus === 'booked') {
      return null
    }

    return {
      listingId,
      startAt: getDateTimeValue(selectedDate, selectedStartHour),
      endAt: getDateTimeValue(
        selectedDate,
        selectedStartHour + resolvedDuration,
      ),
      duration: resolvedDuration,
    }
  }, [
    listingId,
    resolvedDuration,
    selectedDate,
    selectedSlotStatus,
    selectedStartHour,
  ])

  function goToPreviousMonth() {
    const currentMonth = new Date(today.getFullYear(), today.getMonth(), 1)

    if (visibleMonth <= currentMonth) {
      return
    }

    setVisibleMonth(
      new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() - 1, 1),
    )
  }

  function goToNextMonth() {
    setVisibleMonth(
      new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1),
    )
  }

  function handleConfirmBooking() {
    if (!bookingPayload || bookingSubmitting) {
      return
    }

    onConfirmBooking?.(bookingPayload)
  }

  return (
    <section
      className={`${styles.calendar} ${mode === 'preview' ? styles.previewCalendar : ''}`}
    >
      <div className={styles.header}>
        <div>
          <h2>
            {mode === 'preview'
              ? 'Календарь доступности'
              : 'Выберите дату и время'}
          </h2>
          <p>
            Зеленые дни и часы доступны, желтые дни уже имеют отдельные брони,
            красные полностью заняты.
          </p>
        </div>
      </div>

      <div className={styles.legend}>
        <span>
          <i className={styles.availableDot} />
          Доступно
        </span>
        <span>
          <i className={styles.partialDot} />
          Есть записи
        </span>
        <span>
          <i className={styles.bookedDot} />
          Занято
        </span>
      </div>

      <MonthCard
        busyIntervals={busyIntervals}
        calendarDays={calendarDays}
        isCurrentMonth={isCurrentMonth}
        onNextMonth={goToNextMonth}
        onPreviousMonth={goToPreviousMonth}
        onSelectDate={setSelectedDate}
        selectedDate={selectedDate}
        showDayLabels={mode === 'booking'}
        visibleMonth={visibleMonth}
      />

      <TimeSlotsSection
        availabilityError={availabilityError}
        availabilityLoading={availabilityLoading}
        busyIntervals={busyIntervals}
        mode={mode}
        onSelectTime={setSelectedTime}
        selectedDate={selectedDate}
        selectedTime={resolvedSelectedTime}
      />

      {mode === 'booking' && (
        <>
          <DurationSection
            availableDurations={availableDurations}
            duration={resolvedDuration}
            onSelectDuration={setDuration}
          />

          <BookingSummary
            duration={resolvedDuration}
            selectedDate={selectedDate}
            selectedTime={resolvedSelectedTime}
            totalPrice={totalPrice}
          />

          <button
            className={styles.confirmButton}
            type="button"
            disabled={!bookingPayload || bookingSubmitting}
            onClick={handleConfirmBooking}
          >
            {bookingSubmitting ? 'Подтверждаем...' : 'Подтвердить бронирование'}
          </button>
        </>
      )}
    </section>
  )
}
