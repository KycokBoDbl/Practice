import type { BusyInterval } from '../../api/listings'
import { END_HOUR, timeSlots } from './constants'

export type DayStatus = 'available' | 'partial' | 'booked'
export type HourStatus = 'available' | 'booked'

export function toDateValue(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

export function getMonthLabel(date: Date) {
  return date.toLocaleDateString('ru-RU', {
    month: 'long',
    year: 'numeric',
  })
}

export function getDateTimeValue(dateValue: string, hour: number) {
  return `${dateValue}T${String(hour).padStart(2, '0')}:00`
}

export function getIntervalHours(interval: BusyInterval) {
  return {
    startDate: interval.startAt.slice(0, 10),
    endDate: interval.endAt.slice(0, 10),
    startHour: Number(interval.startAt.slice(11, 13)),
    endHour: Number(interval.endAt.slice(11, 13)),
  }
}

export function getDaysOfMonth(date: Date) {
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

export function getDayStatus(dateValue: string, busyIntervals: BusyInterval[]): DayStatus {
  const bookedHours = timeSlots.filter((slot) =>
    getSlotStatus(dateValue, slot, busyIntervals) === 'booked'
  ).length

  if (bookedHours === 0) return 'available'
  if (bookedHours === timeSlots.length) return 'booked'
  return 'partial'
}

export function getSlotStatus(
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

export function getDayStatusLabel(status: DayStatus) {
  if (status === 'available') return 'Доступно'
  if (status === 'partial') return 'Есть записи'
  return 'Занято'
}

export function getHourStatusLabel(status: HourStatus) {
  return status === 'available' ? 'Доступно' : 'Занято'
}

export function getHourFromSlot(slot: string) {
  return Number(slot.split(':')[0])
}

export function getNextBusyHour(
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
