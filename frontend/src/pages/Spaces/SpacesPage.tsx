import { useEffect, useState } from "react";
import { getListings } from "../../api/listings";
import type { Listing } from "../../types/listing";
import { SPACE_TYPE_LABELS } from "../../types/spaceType";

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
    <main>
      <h1>Каталог помещений</h1>

      {listings.length === 0 ? (
        <p>Помещений пока нет.</p>
      ) : (
        <div>
          {listings.map((listing) => (
            <div
              key={listing.id}
              style={{
                border: "1px solid #ccc",
                borderRadius: "8px",
                padding: "16px",
                marginBottom: "16px",
              }}
            >
              <h2>{listing.title}</h2>

              <p>
                <strong>Город:</strong> {listing.city}
              </p>

              <p>
                <strong>Стоимость:</strong>{" "}
                {listing.pricePerHour} ₽/час
              </p>

              <p>
                <strong>Вместимость:</strong>{" "}
                {listing.capacity} человек
              </p>

              <p>
                <strong>Тип:</strong> {SPACE_TYPE_LABELS[listing.spaceType] ?? listing.spaceType}
              </p>

              <img
                src={listing.imageUrl}
                alt={listing.title}
                width={300}
              />
            </div>
          ))}
        </div>
      )}
    </main>
  );
}