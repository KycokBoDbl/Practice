import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'

import { useAuth } from './useAuth'

interface RedirectAuthenticatedProps {
  children: ReactNode
}

export function RedirectAuthenticated({ children }: RedirectAuthenticatedProps) {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return <main>Проверяем авторизацию...</main>
  }

  if (isAuthenticated) {
    return <Navigate to="/profile" replace />
  }

  return children
}
