import { useEffect, useMemo, useState } from 'react'

import { getListingAvailability, type BusyInterval } from '../../api/listings'
import styles from './BookingCalendar.module.css'

interface BookingCalendarProps {
  listingId: number
  pricePerHour: number
  mode?: 'preview' | 'booking'
}

type DayStatus = 'available' | 'partial' | 'booked'
type HourStatus = 'available' | 'booked'

const START_HOUR = 8
const END_HOUR = 18
const timeSlots = Array.from({ length: END_HOUR - START_HOUR }, (_, index) =>
  `${String(START_HOUR + index).padStart(2, '0')}:00`,
)
const durations = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
const weekDays = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']

function toDateValue(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function getMonthLabel(date: Date) {
  return date.toLocaleDateString('ru-RU', {
    month: 'long',
    year: 'numeric',
  })
}

function getDateTimeValue(dateValue: string, hour: number) {
  return `${dateValue}T${String(hour).padStart(2, '0')}:00`
}

function getIntervalHours(interval: BusyInterval) {
  return {
    startDate: interval.startAt.slice(0, 10),
    endDate: interval.endAt.slice(0, 10),
    startHour: Number(interval.startAt.slice(11, 13)),
    endHour: Number(interval.endAt.slice(11, 13)),
  }
}

function getDaysOfMonth(date: Date) {
  const year = date.getFullYear()
  const month = date.getMonth()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const firstDay = new Date(year, month, 1).getDay()
  const offset = firstDay === 0 ? 6 : firstDay - 1

  const emptyCells = Array.from({ length: offset }, () => null)

  const days = Array.from({ length: daysInMonth }, (_, index) => {
    const dayDate = new Date(year, month, index + 1)

    return {
      day: index + 1,
      value: toDateValue(dayDate),
      isPast: dayDate < new Date(new Date().toDateString()),
    }
  })

  return [...emptyCells, ...days]
}

function getDayStatus(dateValue: string, busyIntervals: BusyInterval[]): DayStatus {
  const bookedHours = timeSlots.filter((slot) =>
    getSlotStatus(dateValue, slot, busyIntervals) === 'booked'
  ).length

  if (bookedHours === 0) return 'available'
  if (bookedHours === timeSlots.length) return 'booked'
  return 'partial'
}

function getSlotStatus(
  dateValue: string,
  slot: string,
  busyIntervals: BusyInterval[],
): HourStatus {
  const slotHour = Number(slot.slice(0, 2))

  const booked = busyIntervals.some((interval) => {
    const { startDate, endDate, startHour, endHour } = getIntervalHours(interval)

    return (
      dateValue >= startDate &&
      dateValue <= endDate &&
      getDateTimeValue(dateValue, slotHour) >= interval.startAt &&
      getDateTimeValue(dateValue, slotHour + 1) <= interval.endAt &&
      slotHour >= startHour &&
      slotHour < endHour
    )
  })

  return booked ? 'booked' : 'available'
}

function getDayStatusLabel(status: DayStatus) {
  if (status === 'available') return 'Доступно'
  if (status === 'partial') return 'Есть записи'
  return 'Занято'
}

function getHourStatusLabel(status: HourStatus) {
  return status === 'available' ? 'Доступно' : 'Занято'
}

function getHourFromSlot(slot: string) {
  return Number(slot.split(':')[0])
}

function getNextBusyHour(
  dateValue: string,
  selectedStartHour: number,
  busyIntervals: BusyInterval[],
) {
  const nextBusyInterval = busyIntervals
    .filter((interval) => interval.startAt.slice(0, 10) === dateValue)
    .map((interval) => getIntervalHours(interval))
    .filter((interval) => interval.startHour >= selectedStartHour)
    .sort((left, right) => left.startHour - right.startHour)[0]

  return nextBusyInterval?.startHour ?? END_HOUR
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
  const [busyIntervals, setBusyIntervals] = useState<BusyInterval[]>([])
  const [availabilityLoading, setAvailabilityLoading] = useState(false)

  const calendarDays = useMemo(
    () => getDaysOfMonth(visibleMonth),
    [visibleMonth],
  )

  useEffect(() => {
    async function loadAvailability() {
      const monthStart = new Date(
        visibleMonth.getFullYear(),
        visibleMonth.getMonth(),
        1,
      )
      const nextMonthStart = new Date(
        visibleMonth.getFullYear(),
        visibleMonth.getMonth() + 1,
        1,
      )

      setAvailabilityLoading(true)

      try {
        const data = await getListingAvailability(
          listingId,
          `${toDateValue(monthStart)}T00:00`,
          `${toDateValue(nextMonthStart)}T00:00`,
        )

        setBusyIntervals(data.busyIntervals)
      } catch (error) {
        console.error('Failed to load listing availability:', error)
        setBusyIntervals([])
      } finally {
        setAvailabilityLoading(false)
      }
    }

    loadAvailability()
  }, [listingId, visibleMonth])

  const selectedStartHour = getHourFromSlot(selectedTime)
  const selectedSlotStatus = getSlotStatus(selectedDate, selectedTime, busyIntervals)
  const nextBusyHour = getNextBusyHour(selectedDate, selectedStartHour, busyIntervals)
  const maxDuration =
    selectedSlotStatus === 'booked' ? 0 : Math.max(0, nextBusyHour - selectedStartHour)
  const availableDurations = durations.filter((item) => item <= maxDuration)
  const totalPrice = pricePerHour * duration

  useEffect(() => {
    const firstAvailableSlot = timeSlots.find(
      (slot) => getSlotStatus(selectedDate, slot, busyIntervals) === 'available',
    )

    if (!firstAvailableSlot) {
      return
    }

    if (getSlotStatus(selectedDate, selectedTime, busyIntervals) === 'booked') {
      setSelectedTime(firstAvailableSlot)
    }
  }, [busyIntervals, selectedDate, selectedTime])

  useEffect(() => {
    if (availableDurations.length === 0) {
      setDuration(0)
      return
    }

    if (duration === 0 || duration > maxDuration) {
      setDuration(availableDurations[availableDurations.length - 1])
    }
  }, [availableDurations, duration, maxDuration])

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

      <div className={styles.monthCard}>
        <div className={styles.monthHeader}>
          <button
            type="button"
            className={styles.monthNavButton}
            onClick={goToPreviousMonth}
            disabled={isCurrentMonth}
          >
            ←
          </button>

          <h3 className={styles.monthTitle}>{getMonthLabel(visibleMonth)}</h3>

          <button
            type="button"
            className={styles.monthNavButton}
            onClick={goToNextMonth}
          >
            →
          </button>
        </div>

        <div className={styles.weekDays}>
          {weekDays.map((day) => (
            <span key={day}>{day}</span>
          ))}
        </div>

        <div className={styles.monthGrid}>
          {calendarDays.map((item, index) => {
            if (!item) {
              return <span key={`empty-${index}`} />
            }

            const status = getDayStatus(item.value, busyIntervals)
            const selected = selectedDate === item.value
            const disabled = item.isPast || status === 'booked'

            return (
              <button
                key={item.value}
                type="button"
                disabled={disabled}
                className={[
                  styles.dayButton,
                  styles[status],
                  selected ? styles.selected : '',
                ].join(' ')}
                onClick={() => setSelectedDate(item.value)}
              >
                <span>{item.day}</span>
                <small>
                  {item.isPast ? 'Прошло' : getDayStatusLabel(status)}
                </small>
              </button>
            )
          })}
        </div>
      </div>

      <div className={styles.section}>
        <h3>Доступное время</h3>

        {availabilityLoading && (
          <p className={styles.loading}>Проверяем занятость...</p>
        )}

        <div className={styles.slots}>
          {timeSlots.map((slot) => {
            const status = getSlotStatus(selectedDate, slot, busyIntervals)
            const disabled = status === 'booked' || mode === 'preview'

            return (
              <button
                key={slot}
                type="button"
                disabled={disabled}
                className={[
                  styles.slotButton,
                  styles[status],
                  selectedTime === slot ? styles.selected : '',
                ].join(' ')}
                onClick={() => setSelectedTime(slot)}
              >
                {slot}
                <small>{getHourStatusLabel(status)}</small>
              </button>
            )
          })}
        </div>
      </div>

      {mode === 'booking' && (
        <>
          <div className={styles.section}>
            <h3>Продолжительность</h3>

            {availableDurations.length === 0 ? (
              <p className={styles.loading}>На выбранное время бронирование недоступно.</p>
            ) : (
              <div className={styles.durations}>
                {availableDurations.map((item) => (
                  <button
                    key={item}
                    className={[
                      styles.slotButton,
                      duration === item ? styles.selected : '',
                    ].join(' ')}
                    type="button"
                    onClick={() => setDuration(item)}
                  >
                    {item} ч.
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className={styles.summary}>
            <div>
              <p className={styles.summaryLabel}>Выбранный слот</p>
              <p className={styles.summaryValue}>
                {selectedDate}, {selectedTime}, {duration} ч.
              </p>
            </div>

            <div>
              <p className={styles.summaryLabel}>Итого</p>
              <p className={styles.total}>
                {totalPrice.toLocaleString('ru-RU')} ₽
              </p>
            </div>
          </div>

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
