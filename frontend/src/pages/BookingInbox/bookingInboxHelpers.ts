import type { BookingInboxItem } from '../../types/booking'

export type InboxView = 'all' | 'action-required' | 'confirmed' | 'active' | 'completed'
export type ParticipantRole = 'LANDLORD' | 'TENANT' | string | undefined

const DATE_TIME_FORMAT = new Intl.DateTimeFormat('ru-RU', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

export const BOOKING_STATUS_LABELS: Record<BookingInboxItem['status'], string> = {
  REQUESTED: 'Запрошена',
  AWAITING_CONFIRMATION: 'Ожидает подтверждения',
  CONFIRMED: 'Подтверждена',
  IN_PROGRESS: 'В процессе',
  COMPLETED: 'Завершена',
  REJECTED: 'Отклонена',
  CANCELLED: 'Отменена',
  EXPIRED: 'Истекла',
}

export const INBOX_VIEWS: Array<{ value: InboxView; label: string }> = [
  { value: 'action-required', label: 'Требуют действия' },
  { value: 'confirmed', label: 'Подтвержденные' },
  { value: 'active', label: 'Активные' },
  { value: 'completed', label: 'Завершенные' },
  { value: 'all', label: 'Все' },
]

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return 'Не указано'
  }

  return DATE_TIME_FORMAT.format(new Date(value))
}

export function getCounterpartyLabel(item: BookingInboxItem, role?: ParticipantRole) {
  const organizationName =
    role === 'LANDLORD' ? item.tenantOrganizationName : item.landlordOrganizationName

  return organizationName?.trim() || 'Контрагент не указан'
}

export function isActionRequired(item: BookingInboxItem, role?: ParticipantRole) {
  if (role === 'LANDLORD') {
    return item.status === 'REQUESTED'
  }

  if (role === 'TENANT') {
    return item.status === 'AWAITING_CONFIRMATION'
  }

  return false
}

export function isConfirmedStatus(status: BookingInboxItem['status']) {
  return status === 'CONFIRMED'
}

export function isActiveStatus(status: BookingInboxItem['status']) {
  return status === 'IN_PROGRESS'
}

export function isCompletedStatus(status: BookingInboxItem['status']) {
  return (
    status === 'COMPLETED' ||
    status === 'REJECTED' ||
    status === 'CANCELLED' ||
    status === 'EXPIRED'
  )
}

export function filterInboxItems(
  items: BookingInboxItem[],
  view: InboxView,
  role?: ParticipantRole,
) {
  return items.filter((item) => {
    if (view === 'action-required') {
      return isActionRequired(item, role)
    }

    if (view === 'confirmed') {
      return isConfirmedStatus(item.status)
    }

    if (view === 'active') {
      return isActiveStatus(item.status)
    }

    if (view === 'completed') {
      return isCompletedStatus(item.status)
    }

    return true
  })
}

export function getInboxSummary(items: BookingInboxItem[], role?: ParticipantRole) {
  return {
    total: items.length,
    actionRequired: items.filter((item) => isActionRequired(item, role)).length,
    confirmed: items.filter((item) => isConfirmedStatus(item.status)).length,
    active: items.filter((item) => isActiveStatus(item.status)).length,
    completed: items.filter((item) => isCompletedStatus(item.status)).length,
  }
}

export function getInboxTitle(role?: ParticipantRole) {
  if (role === 'LANDLORD') {
    return 'Полученные заявки'
  }

  if (role === 'TENANT') {
    return 'Отправленные заявки'
  }

  return 'Заявки на бронирование'
}
