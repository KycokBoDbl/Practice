import { useEffect, useState } from "react";
import { Link } from 'react-router-dom'
import { getListings } from "../../api/listings";
import type { Listing } from "../../types/listing";
import { SPACE_TYPE_LABELS } from "../../types/spaceType";
import styles from './SpacesPage.module.css'


export function SpacesPage() {
  const [listings, setListings] = useState<Listing[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadListings() {
      try {
        const data = await getListings();
        setListings(data);
      } catch (error) {
        console.error("Ошибка при загрузке помещений:", error);
      } finally {
        setLoading(false);
      }
    }

    loadListings();
  }, []);

  if (loading) {
    return <p>Загрузка помещений...</p>;
  }

  return (
    <main className={styles.page}>
      <div className={styles.hero}>
        <div>
          <h1 className={styles.title}>Помещения для бизнеса и мероприятий</h1>
          <p className={styles.subtitle}>
            Просматривайте доступные пространства, изучайте
            подробную информацию и выбирайте подходящую площадку
            для встреч, обучения и корпоративных мероприятий.
          </p>
        </div>
        <div className={styles.features}>
          <div className={styles.feature}>
            <span className={styles.featureIcon}>🏢</span>
            <div>
              <h1>Разные типы помещений</h1>
              <p>Переговорные, конференц-залы, классы, лофты и шоурумы.</p>
            </div>
          </div>

          <div className={styles.feature}>
            <span className={styles.featureIcon}>📍</span>
            <div>
              <h1>Несколько городов</h1>
              <p>Выбирайте площадки в Москве, Санкт-Петербурге, Казани и других городах.</p>
            </div>
          </div>

          <div className={styles.feature}>
            <span className={styles.featureIcon}>🕒</span>
            <div>
              <h1>Почасовая аренда</h1>
              <p>Сравнивайте стоимость и подбирайте помещение под нужное время.</p>
            </div>
          </div>
        </div>
      </div>

      <section className={styles.catalogHeader}>
        <div>
          <h2>Каталог помещений</h2>
          <p>Выберите подходящее пространство и перейдите к подробному описанию.</p>
        </div>
      </section>

      {listings.length === 0 ? (
        <p>Помещений пока нет.</p>
      ) : (
        <section className={styles.grid}>
          {listings.map((listing) => (
            <Link
              key={listing.id}
              to={`/spaces/${listing.id}`}
              className={styles.card}
            >
              <img
                className={styles.image}
                src={listing.imageUrl}
                alt={listing.title}
              />

              <div className={styles.content}>
                <h2 className={styles.cardTitle}>{listing.title}</h2>

                <p className={styles.meta}>📍 {listing.city}</p>
                <p className={styles.meta}>👥 до {listing.capacity} человек</p>
                <p className={styles.meta}>
                  🏢 {SPACE_TYPE_LABELS[listing.spaceType] ?? listing.spaceType}
                </p>

                <div className={styles.price}>
                  {listing.pricePerHour.toLocaleString('ru-RU')} ₽/час
                </div>
              </div>
            </Link>
          ))}
        </section>
      )}
    </main>
  )
}