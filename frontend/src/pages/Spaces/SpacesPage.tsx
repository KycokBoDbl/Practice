import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { getListings } from '../../api/listings'
import type { Listing } from '../../types/listing'
import { getSpaceTypeLabel } from '../../types/spaceType'
import { CatalogSearch } from './CatalogSearch'
import { filterCatalogListings, parseCatalogQuery } from './catalogFilters'
import styles from './SpacesPage.module.css'

export function SpacesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [listings, setListings] = useState<Listing[]>([])
  const [loading, setLoading] = useState(true)
  const catalogQuery = useMemo(
    () => parseCatalogQuery(searchParams),
    [searchParams],
  )

  useEffect(() => {
    async function loadListings() {
      try {
        const data = await getListings()
        setListings(data)
      } catch (error) {
        console.error('РћС€РёР±РєР° РїСЂРё Р·Р°РіСЂСѓР·РєРµ РїРѕРјРµС‰РµРЅРёР№:', error)
      } finally {
        setLoading(false)
      }
    }

    loadListings()
  }, [])

  const filteredListings = useMemo(
    () => filterCatalogListings(listings, catalogQuery),
    [catalogQuery, listings],
  )

  function resetFilters() {
    setSearchParams({})
  }

  if (loading) {
    return <p>Р—Р°РіСЂСѓР·РєР° РїРѕРјРµС‰РµРЅРёР№...</p>
  }

  return (
    <main className={styles.page}>
      <div className={styles.hero}>
        <div>
          <h1 className={styles.title}>РџРѕРјРµС‰РµРЅРёСЏ РґР»СЏ Р±РёР·РЅРµСЃР° Рё РјРµСЂРѕРїСЂРёСЏС‚РёР№</h1>
          <p className={styles.subtitle}>
            РџСЂРѕСЃРјР°С‚СЂРёРІР°Р№С‚Рµ РґРѕСЃС‚СѓРїРЅС‹Рµ РїСЂРѕСЃС‚СЂР°РЅСЃС‚РІР°, РёР·СѓС‡Р°Р№С‚Рµ
            РїРѕРґСЂРѕР±РЅСѓСЋ РёРЅС„РѕСЂРјР°С†РёСЋ Рё РІС‹Р±РёСЂР°Р№С‚Рµ РїРѕРґС…РѕРґСЏС‰СѓСЋ РїР»РѕС‰Р°РґРєСѓ
            РґР»СЏ РІСЃС‚СЂРµС‡, РѕР±СѓС‡РµРЅРёСЏ Рё РєРѕСЂРїРѕСЂР°С‚РёРІРЅС‹С… РјРµСЂРѕРїСЂРёСЏС‚РёР№.
          </p>
        </div>
        <div className={styles.features}>
          <div className={styles.feature}>
            <span className={styles.featureIcon}>рџЏў</span>
            <div>
              <h3>Разные типы помещений</h3>
              <p>РџРµСЂРµРіРѕРІРѕСЂРЅС‹Рµ, РєРѕРЅС„РµСЂРµРЅС†-Р·Р°Р»С‹, РєР»Р°СЃСЃС‹, Р»РѕС„С‚С‹ Рё С€РѕСѓСЂСѓРјС‹.</p>
            </div>
          </div>

          <div className={styles.feature}>
            <span className={styles.featureIcon}>рџ“Ќ</span>
            <div>
              <h3>Несколько городов</h3>
              <p>Р’С‹Р±РёСЂР°Р№С‚Рµ РїР»РѕС‰Р°РґРєРё РІ РњРѕСЃРєРІРµ, РЎР°РЅРєС‚-РџРµС‚РµСЂР±СѓСЂРіРµ, РљР°Р·Р°РЅРё Рё РґСЂСѓРіРёС… РіРѕСЂРѕРґР°С….</p>
            </div>
          </div>

          <div className={styles.feature}>
            <span className={styles.featureIcon}>рџ•’</span>
            <div>
              <h3>Почасовая аренда</h3>
              <p>РЎСЂР°РІРЅРёРІР°Р№С‚Рµ СЃС‚РѕРёРјРѕСЃС‚СЊ Рё РїРѕРґР±РёСЂР°Р№С‚Рµ РїРѕРјРµС‰РµРЅРёРµ РїРѕРґ РЅСѓР¶РЅРѕРµ РІСЂРµРјСЏ.</p>
            </div>
          </div>
        </div>
      </div>

      <section className={styles.catalogHeader} id="catalog">
        <div>
          <h2>РљР°С‚Р°Р»РѕРі РїРѕРјРµС‰РµРЅРёР№</h2>
          <p>Р’С‹Р±РµСЂРёС‚Рµ РїРѕРґС…РѕРґСЏС‰РµРµ РїСЂРѕСЃС‚СЂР°РЅСЃС‚РІРѕ Рё РїРµСЂРµР№РґРёС‚Рµ Рє РїРѕРґСЂРѕР±РЅРѕРјСѓ РѕРїРёСЃР°РЅРёСЋ.</p>
        </div>
      </section>

      <CatalogSearch listings={listings} />

      {listings.length === 0 ? (
        <p>РџРѕРјРµС‰РµРЅРёР№ РїРѕРєР° РЅРµС‚.</p>
      ) : filteredListings.length === 0 ? (
        <div className={styles.emptyState}>
          <p>РџРѕ РІС‹Р±СЂР°РЅРЅС‹Рј С„РёР»СЊС‚СЂР°Рј РїРѕРјРµС‰РµРЅРёР№ РЅРµС‚.</p>
          <button type="button" className={styles.resetButton} onClick={resetFilters}>
            РЎР±СЂРѕСЃРёС‚СЊ С„РёР»СЊС‚СЂС‹
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

                <p className={styles.meta}>рџ“Ќ {listing.city}</p>
                <p className={styles.meta}>рџ‘Ґ РґРѕ {listing.capacity} С‡РµР»РѕРІРµРє</p>
                <p className={styles.meta}>
                  рџЏў {getSpaceTypeLabel(listing.spaceType)}
                </p>

                <div className={styles.price}>
                  {listing.pricePerHour.toLocaleString('ru-RU')} в‚Ѕ/С‡Р°СЃ
                </div>
              </div>
            </Link>
          ))}
        </section>
      )}
    </main>
  )
}

