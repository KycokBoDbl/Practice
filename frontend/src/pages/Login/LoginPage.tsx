import type { FormEvent } from 'react'
import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'

import { parseApiError } from '../../api/problemDetails'
import { useAuth } from '../../auth/useAuth'
import styles from './LoginPage.module.css'

interface LoginFormErrors {
  email?: string
  password?: string
  form?: string
}

const INVALID_CREDENTIALS_MESSAGE = 'Неверный email или пароль.'

interface LoginLocationState {
  registrationSuccess?: boolean
  registeredEmail?: string
}

export function LoginPage() {
  const location = useLocation()
  const locationState = location.state as LoginLocationState | null
  const { login } = useAuth()
  const [email, setEmail] = useState(locationState?.registeredEmail ?? '')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<LoginFormErrors>({})
  const [successMessage, setSuccessMessage] = useState(
    locationState?.registrationSuccess
      ? 'Регистрация завершена. Теперь войдите в аккаунт.'
      : '',
  )
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    setSubmitting(true)
    setErrors({})
    setSuccessMessage('')

    try {
      await login({ email, password })
      setSuccessMessage('Вход выполнен.')
    } catch (error) {
      const parsedError = parseApiError(error)
      const fieldErrors = parsedError.fieldErrors

      setErrors({
        email: fieldErrors.email,
        password: fieldErrors.password,
        form:
          parsedError.status === 401
            ? INVALID_CREDENTIALS_MESSAGE
            : parsedError.message,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className={styles.page}>
      <section className={styles.panel}>
        <h1>Вход</h1>

        <form className={styles.form} onSubmit={handleSubmit} noValidate>
          <label className={styles.field}>
            <span>Email</span>
            <input
              type="email"
              name="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={errors.email ? 'true' : undefined}
              aria-describedby={errors.email ? 'login-email-error' : undefined}
            />
            {errors.email && (
              <small id="login-email-error" className={styles.error}>
                {errors.email}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>Пароль</span>
            <input
              type="password"
              name="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-invalid={errors.password ? 'true' : undefined}
              aria-describedby={errors.password ? 'login-password-error' : undefined}
            />
            {errors.password && (
              <small id="login-password-error" className={styles.error}>
                {errors.password}
              </small>
            )}
          </label>

          {errors.form && (
            <p className={styles.error} role="alert">
              {errors.form}
            </p>
          )}

          {successMessage && (
            <p className={styles.success} role="status">
              {successMessage}
            </p>
          )}

          <button type="submit" className={styles.button} disabled={submitting}>
            {submitting ? 'Входим...' : 'Войти'}
          </button>
        </form>

        <p className={styles.secondaryAction}>
          Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
        </p>
      </section>
    </main>
  )
}
