import type { BusyInterval } from '../../api/listings'
import { timeSlots, weekDays } from './constants'
import styles from './BookingCalendar.module.css'
import {
  getDayStatus,
  getDayStatusLabel,
  getHourStatusLabel,
  getMonthLabel,
  getSlotStatus,
} from './utils'

type CalendarDay = {
  day: number
  value: string
  isPast: boolean
} | null

interface MonthCardProps {
  busyIntervals: BusyInterval[]
  calendarDays: CalendarDay[]
  isCurrentMonth: boolean
  onNextMonth: () => void
  onPreviousMonth: () => void
  onSelectDate: (dateValue: string) => void
  selectedDate: string
  visibleMonth: Date
}

export function MonthCard({
  busyIntervals,
  calendarDays,
  isCurrentMonth,
  onNextMonth,
  onPreviousMonth,
  onSelectDate,
  selectedDate,
  visibleMonth,
}: MonthCardProps) {
  return (
    <div className={styles.monthCard}>
      <div className={styles.monthHeader}>
        <button
          type="button"
          className={styles.monthNavButton}
          onClick={onPreviousMonth}
          disabled={isCurrentMonth}
        >
          ←
        </button>

        <h3 className={styles.monthTitle}>{getMonthLabel(visibleMonth)}</h3>

        <button
          type="button"
          className={styles.monthNavButton}
          onClick={onNextMonth}
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
              onClick={() => onSelectDate(item.value)}
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
  )
}

interface TimeSlotsSectionProps {
  availabilityLoading: boolean
  busyIntervals: BusyInterval[]
  mode: 'preview' | 'booking'
  onSelectTime: (time: string) => void
  selectedDate: string
  selectedTime: string
}

export function TimeSlotsSection({
  availabilityLoading,
  busyIntervals,
  mode,
  onSelectTime,
  selectedDate,
  selectedTime,
}: TimeSlotsSectionProps) {
  return (
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
              onClick={() => onSelectTime(slot)}
            >
              {slot}
              <small>{getHourStatusLabel(status)}</small>
            </button>
          )
        })}
      </div>
    </div>
  )
}

interface DurationSectionProps {
  availableDurations: number[]
  duration: number
  onSelectDuration: (duration: number) => void
}

export function DurationSection({
  availableDurations,
  duration,
  onSelectDuration,
}: DurationSectionProps) {
  return (
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
              onClick={() => onSelectDuration(item)}
            >
              {item} ч.
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

interface BookingSummaryProps {
  duration: number
  selectedDate: string
  selectedTime: string
  totalPrice: number
}

export function BookingSummary({
  duration,
  selectedDate,
  selectedTime,
  totalPrice,
}: BookingSummaryProps) {
  return (
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
  )
}
