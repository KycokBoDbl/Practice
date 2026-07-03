import type { TokenResponse } from '../types/auth'

const AUTH_TOKEN_STORAGE_KEY = 'roomhub.auth.token'
const clearListeners = new Set<() => void>()

export interface StoredAuthToken {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: number
}

function getLocalStorage() {
  try {
    return typeof window === 'undefined' ? null : window.localStorage
  } catch {
    return null
  }
}

function isStoredAuthToken(value: unknown): value is StoredAuthToken {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Partial<StoredAuthToken>

  return (
    typeof candidate.accessToken === 'string' &&
    candidate.accessToken.length > 0 &&
    candidate.tokenType === 'Bearer' &&
    typeof candidate.expiresAt === 'number' &&
    Number.isFinite(candidate.expiresAt)
  )
}

export function createStoredAuthToken(
  token: TokenResponse,
  now = Date.now(),
): StoredAuthToken {
  return {
    accessToken: token.accessToken,
    tokenType: token.tokenType,
    expiresAt: now + token.expiresIn * 1000,
  }
}

export function clearStoredAuthToken() {
  getLocalStorage()?.removeItem(AUTH_TOKEN_STORAGE_KEY)

  for (const listener of clearListeners) {
    listener()
  }
}

export function saveStoredAuthToken(token: TokenResponse) {
  const storage = getLocalStorage()

  if (!storage) {
    return null
  }

  const storedToken = createStoredAuthToken(token)
  storage.setItem(AUTH_TOKEN_STORAGE_KEY, JSON.stringify(storedToken))

  return storedToken
}

export function getStoredAuthToken(now = Date.now()): StoredAuthToken | null {
  const storage = getLocalStorage()

  if (!storage) {
    return null
  }

  const rawToken = storage.getItem(AUTH_TOKEN_STORAGE_KEY)

  if (!rawToken) {
    return null
  }

  try {
    const parsedToken: unknown = JSON.parse(rawToken)

    if (!isStoredAuthToken(parsedToken) || parsedToken.expiresAt <= now) {
      clearStoredAuthToken()
      return null
    }

    return parsedToken
  } catch {
    clearStoredAuthToken()
    return null
  }
}

export function getAuthorizationHeader() {
  const token = getStoredAuthToken()

  return token ? `${token.tokenType} ${token.accessToken}` : null
}

export function subscribeAuthTokenCleared(listener: () => void) {
  clearListeners.add(listener)

  return () => {
    clearListeners.delete(listener)
  }
}

