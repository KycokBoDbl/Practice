import { NavLink } from 'react-router-dom'

import { useAuth } from '../../auth/useAuth'
import styles from './Header.module.css'

function getLinkClass(isActive: boolean) {
  return isActive ? `${styles.link} ${styles.active}` : styles.link
}

export function Header() {
  const { isAuthenticated, logout, profile } = useAuth()

  function handleLogout() {
    if (window.confirm('Выйти из аккаунта?')) {
      logout()
    }
  }

  return (
    <header className={styles.header}>
      <strong className={styles.logo}>RoomHub</strong>

      <nav className={styles.nav}>
        <NavLink to="/" className={({ isActive }) => getLinkClass(isActive)}>
          Помещения
        </NavLink>

        {isAuthenticated ? (
          <>
            {profile?.role === 'LANDLORD' && (
              <>
                <NavLink
                  to="/spaces/new"
                  className={({ isActive }) => getLinkClass(isActive)}
                >
                  Опубликовать
                </NavLink>

                <NavLink
                  to="/my-listings"
                  className={({ isActive }) => getLinkClass(isActive)}
                >
                  Мои объявления
                </NavLink>
              </>
            )}

            <NavLink to="/bookings" className={({ isActive }) => getLinkClass(isActive)}>
              Заявки
            </NavLink>

            <NavLink to="/profile" className={({ isActive }) => getLinkClass(isActive)}>
              Профиль
            </NavLink>

            <button type="button" className={styles.linkButton} onClick={handleLogout}>
              Выйти
            </button>
          </>
        ) : (
          <>
            <NavLink to="/login" className={({ isActive }) => getLinkClass(isActive)}>
              Войти
            </NavLink>

            <NavLink to="/register" className={({ isActive }) => getLinkClass(isActive)}>
              Регистрация
            </NavLink>
          </>
        )}
      </nav>
    </header>
  )
}
