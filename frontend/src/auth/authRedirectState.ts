export interface AuthRedirectState {
  registrationSuccess?: boolean
  registeredEmail?: string
  returnTo?: string
}

export function getSafeReturnTo(returnTo: unknown) {
  if (typeof returnTo !== 'string') {
    return null
  }

  if (!returnTo.startsWith('/') || returnTo.startsWith('//')) {
    return null
  }

  if (returnTo === '/login' || returnTo === '/register') {
    return null
  }

  return returnTo
}

export function getAuthRedirectState(state: unknown): AuthRedirectState {
  if (!state || typeof state !== 'object') {
    return {}
  }

  const record = state as Record<string, unknown>

  return {
    registrationSuccess:
      typeof record.registrationSuccess === 'boolean'
        ? record.registrationSuccess
        : undefined,
    registeredEmail:
      typeof record.registeredEmail === 'string'
        ? record.registeredEmail
        : undefined,
    returnTo: getSafeReturnTo(record.returnTo) ?? undefined,
  }
}
