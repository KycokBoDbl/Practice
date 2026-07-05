import type { FormEvent } from 'react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { parseApiError } from '../../api/problemDetails'
import { getAuthRedirectState } from '../../auth/authRedirectState'
import { useAuth } from '../../auth/useAuth'
import type { UserRole } from '../../types/auth'
import styles from './RegisterPage.module.css'

interface RegisterFormErrors {
  role?: string
  legalName?: string
  taxId?: string
  email?: string
  password?: string
  form?: string
}

const PASSWORD_MIN_LENGTH = 8
const PASSWORD_MAX_LENGTH = 64
const PASSWORD_ERROR_MESSAGE = `Пароль должен содержать от ${PASSWORD_MIN_LENGTH} до ${PASSWORD_MAX_LENGTH} символов.`

const ROLE_OPTIONS: Array<{ value: UserRole; label: string }> = [
  { value: 'TENANT', label: 'Арендатор' },
  { value: 'LANDLORD', label: 'Арендодатель' },
]

function getRegistrationFieldError(
  fieldName: keyof Omit<RegisterFormErrors, 'form'>,
  fieldErrors: Record<string, string>,
) {
  const message = fieldErrors[fieldName]

  if (!message) {
    return undefined
  }

  if (fieldName === 'taxId') {
    return 'ИНН должен состоять из 10 цифр.'
  }

  if (fieldName === 'password') {
    return PASSWORD_ERROR_MESSAGE
  }

  return message
}

function validatePassword(password: string) {
  return password.length >= PASSWORD_MIN_LENGTH && password.length <= PASSWORD_MAX_LENGTH
}

export function RegisterPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const locationState = getAuthRedirectState(location.state)
  const { register } = useAuth()
  const [role, setRole] = useState<UserRole>('TENANT')
  const [legalName, setLegalName] = useState('')
  const [taxId, setTaxId] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<RegisterFormErrors>({})
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!validatePassword(password)) {
      setErrors({ password: PASSWORD_ERROR_MESSAGE })
      return
    }

    setSubmitting(true)
    setErrors({})

    try {
      await register({
        role,
        legalName,
        taxId,
        email,
        password,
      })

      navigate('/login', {
        replace: true,
        state: {
          registrationSuccess: true,
          registeredEmail: email,
          returnTo: locationState.returnTo,
        },
      })
    } catch (error) {
      const parsedError = parseApiError(error)
      const fieldErrors = parsedError.fieldErrors

      setErrors({
        role: getRegistrationFieldError('role', fieldErrors),
        legalName: getRegistrationFieldError('legalName', fieldErrors),
        taxId: getRegistrationFieldError('taxId', fieldErrors),
        email: getRegistrationFieldError('email', fieldErrors),
        password: getRegistrationFieldError('password', fieldErrors),
        form: parsedError.message,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className={styles.page}>
      <section className={styles.panel}>
        <h1>Регистрация</h1>

        <form className={styles.form} onSubmit={handleSubmit} noValidate>
          <label className={styles.field}>
            <span>Роль</span>
            <select
              name="role"
              value={role}
              onChange={(event) => setRole(event.target.value as UserRole)}
              aria-invalid={errors.role ? 'true' : undefined}
              aria-describedby={errors.role ? 'register-role-error' : undefined}
            >
              {ROLE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {errors.role && (
              <small id="register-role-error" className={styles.error}>
                {errors.role}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>Юридическое название</span>
            <input
              type="text"
              name="legalName"
              autoComplete="organization"
              value={legalName}
              onChange={(event) => setLegalName(event.target.value)}
              aria-invalid={errors.legalName ? 'true' : undefined}
              aria-describedby={errors.legalName ? 'register-legal-name-error' : undefined}
            />
            {errors.legalName && (
              <small id="register-legal-name-error" className={styles.error}>
                {errors.legalName}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>ИНН</span>
            <input
              type="text"
              name="taxId"
              inputMode="numeric"
              autoComplete="off"
              value={taxId}
              onChange={(event) => setTaxId(event.target.value)}
              aria-invalid={errors.taxId ? 'true' : undefined}
              aria-describedby={errors.taxId ? 'register-tax-id-error' : undefined}
            />
            {errors.taxId && (
              <small id="register-tax-id-error" className={styles.error}>
                {errors.taxId}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>Email</span>
            <input
              type="email"
              name="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={errors.email ? 'true' : undefined}
              aria-describedby={errors.email ? 'register-email-error' : undefined}
            />
            {errors.email && (
              <small id="register-email-error" className={styles.error}>
                {errors.email}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>Пароль</span>
            <input
              type="password"
              name="password"
              autoComplete="new-password"
              minLength={PASSWORD_MIN_LENGTH}
              maxLength={PASSWORD_MAX_LENGTH}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-invalid={errors.password ? 'true' : undefined}
              aria-describedby={errors.password ? 'register-password-error' : undefined}
            />
            {errors.password && (
              <small id="register-password-error" className={styles.error}>
                {errors.password}
              </small>
            )}
          </label>

          {errors.form && (
            <p className={styles.error} role="alert">
              {errors.form}
            </p>
          )}

          <button type="submit" className={styles.button} disabled={submitting}>
            {submitting ? 'Создаем аккаунт...' : 'Зарегистрироваться'}
          </button>
        </form>

        <p className={styles.secondaryAction}>
          Уже есть аккаунт? <Link to="/login" state={locationState}>Войти</Link>
        </p>
      </section>
    </main>
  )
}
