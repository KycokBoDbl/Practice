import { useMemo, useState } from 'react'

import { durations, timeSlots } from './constants'
import styles from './BookingCalendar.module.css'
import {
  BookingSummary,
  DurationSection,
  MonthCard,
  TimeSlotsSection,
} from './BookingCalendarSections'
import { useListingAvailability } from './useListingAvailability'
import {
  getDaysOfMonth,
  getHourFromSlot,
  getNextBusyHour,
  getSlotStatus,
  toDateValue,
} from './utils'

interface BookingCalendarProps {
  listingId: number
  pricePerHour: number
  mode?: 'preview' | 'booking'
}

export function BookingCalendar({
  listingId,
  pricePerHour,
  mode = 'booking',
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
  const { availabilityLoading, busyIntervals } = useListingAvailability(
    listingId,
    visibleMonth,
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

  return (
    <section className={styles.calendar}>
      <div className={styles.header}>
        <div>
          <h2>
            {mode === 'preview'
              ? 'Календарь доступности'
              : 'Выберите дату и время'}
          </h2>
          <p>
            Зеленые дни и часы доступны, желтые дни уже имеют отдельные записи,
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
        visibleMonth={visibleMonth}
      />

      <TimeSlotsSection
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
            disabled={availableDurations.length === 0}
          >
            Подтвердить бронирование
          </button>
        </>
      )}
    </section>
  )
}
