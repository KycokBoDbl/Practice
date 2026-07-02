export const START_HOUR = 8
export const END_HOUR = 18

export const timeSlots = Array.from({ length: END_HOUR - START_HOUR }, (_, index) =>
  `${String(START_HOUR + index).padStart(2, '0')}:00`,
)

export const durations = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
export const weekDays = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']
