import { AMENITY_GROUPS } from './listingPublicationOptions'
import styles from './ListingPublicationPage.module.css'

interface PublicationAmenityGroupsProps {
  selectedAmenities: string[]
  onToggleAmenity: (amenity: string) => void
}

export function PublicationAmenityGroups({
  onToggleAmenity,
  selectedAmenities,
}: PublicationAmenityGroupsProps) {
  return (
    <div className={styles.amenityGroups}>
      {AMENITY_GROUPS.map((group) => (
        <fieldset key={group.title} className={styles.amenityGroup}>
          <legend>{group.title}</legend>
          <div className={styles.chips}>
            {group.options.map((amenity) => {
              const selected = selectedAmenities.includes(amenity)

              return (
                <button
                  key={amenity}
                  type="button"
                  className={`${styles.chip} ${selected ? styles.chipSelected : ''}`}
                  onClick={() => onToggleAmenity(amenity)}
                  aria-pressed={selected}
                >
                  {amenity}
                </button>
              )
            })}
          </div>
        </fieldset>
      ))}
    </div>
  )
}
