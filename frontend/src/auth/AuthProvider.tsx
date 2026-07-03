import type { ReactNode } from 'react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import axios from 'axios'

import {
  getMe,
  login as loginRequest,
  register as registerRequest,
} from '../api/auth'
import {
  clearStoredAuthToken,
  getStoredAuthToken,
  saveStoredAuthToken,
  subscribeAuthTokenCleared,
} from '../api/authTokenStorage'
import type {
  LoginRequest,
  ProfileResponse,
  RegisterRequest,
} from '../types/auth'
import { AuthContext } from './AuthContext'

interface AuthProviderProps {
  children: ReactNode
}

function isUnauthorizedError(error: unknown) {
  return axios.isAxiosError(error) && error.response?.status === 401
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [profile, setProfile] = useState<ProfileResponse | null>(null)
  const [loading, setLoading] = useState(true)

  const logout = useCallback(() => {
    clearStoredAuthToken()
    setProfile(null)
    setLoading(false)
  }, [])

  const refreshProfile = useCallback(async () => {
    try {
      const nextProfile = await getMe()
      setProfile(nextProfile)
      return nextProfile
    } catch (error) {
      if (isUnauthorizedError(error)) {
        clearStoredAuthToken()
        setProfile(null)
      }

      throw error
    }
  }, [])

  const login = useCallback(async (request: LoginRequest) => {
    clearStoredAuthToken()
    setProfile(null)

    const token = await loginRequest(request)
    saveStoredAuthToken(token)

    try {
      const nextProfile = await getMe()
      setProfile(nextProfile)
      return nextProfile
    } catch (error) {
      if (isUnauthorizedError(error)) {
        clearStoredAuthToken()
        setProfile(null)
      }

      throw error
    }
  }, [])

  const register = useCallback((request: RegisterRequest) => {
    return registerRequest(request)
  }, [])

  useEffect(() => {
    return subscribeAuthTokenCleared(() => {
      setProfile(null)
    })
  }, [])

  useEffect(() => {
    let cancelled = false

    async function hydrateAuthState() {
      const storedToken = getStoredAuthToken()

      if (!storedToken) {
        setLoading(false)
        return
      }

      try {
        const nextProfile = await getMe()

        if (!cancelled) {
          setProfile(nextProfile)
        }
      } catch (error) {
        if (!cancelled && isUnauthorizedError(error)) {
          clearStoredAuthToken()
          setProfile(null)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    hydrateAuthState()

    return () => {
      cancelled = true
    }
  }, [])

  const value = useMemo(
    () => ({
      profile,
      loading,
      isAuthenticated: profile !== null,
      login,
      register,
      refreshProfile,
      logout,
    }),
    [loading, login, logout, profile, refreshProfile, register],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

