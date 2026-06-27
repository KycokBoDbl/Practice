import { NavLink } from 'react-router-dom'

import styles from './Header.module.css'

export function Header() {
  return (
    <header className={styles.header}>
      <strong className={styles.logo}>RoomHub</strong>

      <nav className={styles.nav}>
        <NavLink to="/" className={({ isActive }) => (isActive ? `${styles.link} ${styles.active}` : styles.link)}>
          Помещения
        </NavLink>

        <NavLink to="/profile" className={({ isActive }) => (isActive ? `${styles.link} ${styles.active}` : styles.link)}>
          Профиль
        </NavLink>

        <NavLink to="/login" className={({ isActive }) => (isActive ? `${styles.link} ${styles.active}` : styles.link)}>
          Войти
        </NavLink>
      </nav>
    </header>
  )
}