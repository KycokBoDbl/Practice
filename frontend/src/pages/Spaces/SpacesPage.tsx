import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from 'react-router-dom'
import { getListings } from "../../api/listings";
import type { Listing } from "../../types/listing";
import { SPACE_TYPE_LABELS } from "../../types/spaceType";
import styles from './SpacesPage.module.css'


export function SpacesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [listings, setListings] = useState<Listing[]>([]);
  const [loading, setLoading] = useState(true);
  const searchQuery = (searchParams.get('q') ?? '').trim().toLowerCase();
  const searchTokens = useMemo(
    () => searchQuery.split(/\s+/).filter((token) => token.length >= 3),
    [searchQuery],
  );
  const selectedCity = searchParams.get('city') ?? '';
  const minCapacity = searchParams.get('minCapacity') ?? '';
  const maxCapacity = searchParams.get('maxCapacity') ?? '';
  const minPrice = searchParams.get('minPrice') ?? '';
  const maxPrice = searchParams.get('maxPrice') ?? '';

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

  const filteredListings = useMemo(() => {
    const minCapacityValue = Number(minCapacity);
    const maxCapacityValue = Number(maxCapacity);
    const minPriceValue = Number(minPrice);
    const maxPriceValue = Number(maxPrice);

    return listings.filter((listing) => {
      const searchText = [
        listing.title,
        listing.description,
        listing.city,
        listing.address,
        listing.spaceType,
        SPACE_TYPE_LABELS[listing.spaceType],
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      const matchesSearch =
        searchTokens.length === 0 ||
        searchTokens.every((token) => searchText.includes(token));
      const matchesCity = selectedCity === '' || listing.city === selectedCity;
      const matchesMinCapacity =
        minCapacity === '' || listing.capacity >= minCapacityValue;
      const matchesMaxCapacity =
        maxCapacity === '' || listing.capacity <= maxCapacityValue;
      const matchesMinPrice =
        minPrice === '' || listing.pricePerHour >= minPriceValue;
      const matchesMaxPrice =
        maxPrice === '' || listing.pricePerHour <= maxPriceValue;

      return (
        matchesSearch &&
        matchesCity &&
        matchesMinCapacity &&
        matchesMaxCapacity &&
        matchesMinPrice &&
        matchesMaxPrice
      );
    });
  }, [listings, maxCapacity, maxPrice, minCapacity, minPrice, searchTokens, selectedCity]);

  function resetFilters() {
    setSearchParams({});
  }

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

      <section className={styles.catalogHeader} id="catalog">
        <div>
          <h2>Каталог помещений</h2>
          <p>Выберите подходящее пространство и перейдите к подробному описанию.</p>
        </div>
      </section>

      {listings.length === 0 ? (
        <p>Помещений пока нет.</p>
      ) : filteredListings.length === 0 ? (
        <div className={styles.emptyState}>
          <p>По выбранным фильтрам помещений нет.</p>
          <button type="button" className={styles.resetButton} onClick={resetFilters}>
            Сбросить фильтры
          </button>
        </div>
      ) : (
        <section className={styles.grid}>
          {filteredListings.map((listing) => (
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
