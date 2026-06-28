import { useEffect, useMemo, useState } from 'react'

import styles from './BookingCalendar.module.css'

interface BookingCalendarProps {
  pricePerHour: number
  mode?: 'preview' | 'booking'
}

type SlotStatus = 'available' | 'partial' | 'booked'

const timeSlots = [
  '08:00',
  '09:00',
  '10:00',
  '11:00',
  '12:00',
  '14:00',
  '15:00',
  '16:00',
  '17:00',
]

const durations = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
const weekDays = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']

function toDateValue(date: Date) {
  return date.toISOString().slice(0, 10)
}

function getMonthLabel(date: Date) {
  return date.toLocaleDateString('ru-RU', {
    month: 'long',
    year: 'numeric',
  })
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

function getDayStatus(): SlotStatus {
  return 'available'
}

function getSlotStatus(): SlotStatus {
  return 'available'
}

function getStatusLabel(status: SlotStatus) {
  if (status === 'available') return 'Доступно'
  if (status === 'partial') return 'Частично занято'
  return 'Занято'
}

function getHourFromSlot(slot: string) {
  return Number(slot.split(':')[0])
}

export function BookingCalendar({
  pricePerHour,
  mode = 'booking',
}: BookingCalendarProps) {
  const today = new Date()

  const [visibleMonth, setVisibleMonth] = useState(
    () => new Date(today.getFullYear(), today.getMonth(), 1),
  )
  const currentMonth = new Date(
      today.getFullYear(),
      today.getMonth(),
      1,
    )

  const isCurrentMonth =
    visibleMonth.getFullYear() === currentMonth.getFullYear() &&
    visibleMonth.getMonth() === currentMonth.getMonth()

  const [selectedDate, setSelectedDate] = useState(toDateValue(today))
  const [selectedTime, setSelectedTime] = useState('09:00')
  const [duration, setDuration] = useState(2)

  const calendarDays = useMemo(
    () => getDaysOfMonth(visibleMonth),
    [visibleMonth],
  )

  const totalPrice = pricePerHour * duration

  const lastAvailableHour = getHourFromSlot(timeSlots[timeSlots.length - 1]) + 1
  const selectedStartHour = getHourFromSlot(selectedTime)
  const maxDuration = lastAvailableHour - selectedStartHour

  const availableDurations = durations.filter((item) => item <= maxDuration)

  useEffect(() => {
    if (duration > maxDuration) {
      setDuration(maxDuration)
    }
  }, [duration, maxDuration])

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
            Выберите удобную дату и время. Проверка занятости слотов будет
            добавлена на следующем этапе.
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
          Частично занято
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

            const status = getDayStatus()
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
                <small>{item.isPast ? 'Прошло' : getStatusLabel(status)}</small>
              </button>
            )
          })}
        </div>
      </div>

      <div className={styles.section}>
        <h3>Доступное время</h3>

        <div className={styles.slots}>
          {timeSlots.map((slot) => {
            const status = getSlotStatus()
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
                <small>{getStatusLabel(status)}</small>
              </button>
            )
          })}
        </div>
      </div>

      {mode === 'booking' && (
        <>
          <div className={styles.section}>
            <h3>Продолжительность</h3>

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

          <button className={styles.confirmButton} type="button">
            Подтвердить бронирование
          </button>
        </>
      )}
    </section>
  )
}