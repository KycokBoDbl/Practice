import { useAuth } from '../../auth/useAuth'
import styles from './ProfilePage.module.css'

const ROLE_LABELS = {
  LANDLORD: 'Арендодатель',
  TENANT: 'Арендатор',
} as const

export function ProfilePage() {
  const { profile } = useAuth()

  if (!profile) {
    return <main className={styles.page}>Профиль недоступен.</main>
  }

  return (
    <main className={styles.page}>
      <section className={styles.panel}>
        <h1>Профиль</h1>

        <dl className={styles.details}>
          <div>
            <dt>Организация</dt>
            <dd>{profile.legalName}</dd>
          </div>

          <div>
            <dt>ИНН</dt>
            <dd>{profile.taxId}</dd>
          </div>

          <div>
            <dt>Email</dt>
            <dd>{profile.email}</dd>
          </div>

          <div>
            <dt>Роль</dt>
            <dd>{ROLE_LABELS[profile.role]}</dd>
          </div>
        </dl>
      </section>
    </main>
  )
}

