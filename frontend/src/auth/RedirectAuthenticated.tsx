import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

import { getAuthRedirectState } from './authRedirectState'
import { useAuth } from './useAuth'

interface RedirectAuthenticatedProps {
  children: ReactNode
}

export function RedirectAuthenticated({ children }: RedirectAuthenticatedProps) {
  const { isAuthenticated, loading } = useAuth()
  const location = useLocation()
  const locationState = getAuthRedirectState(location.state)

  if (loading) {
    return <main>Проверяем авторизацию...</main>
  }

  if (isAuthenticated) {
    return <Navigate to={locationState.returnTo ?? '/profile'} replace />
  }

  return children
}
