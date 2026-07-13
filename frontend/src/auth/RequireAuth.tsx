import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

import { getReturnToFromLocation } from './authRedirectState'
import { useAuth } from './useAuth'

interface RequireAuthProps {
  children: ReactNode
}

export function RequireAuth({ children }: RequireAuthProps) {
  const { isAuthenticated, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <main>Проверяем авторизацию...</main>
  }

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ returnTo: getReturnToFromLocation(location) }}
      />
    )
  }

  return children
}

