import { useCallback, useState } from 'react'

import {
  INITIAL_FORM_STATE,
  type PublicationFormErrors,
  type PublicationFormState,
} from './listingPublicationForm'

export function useListingPublicationForm() {
  const [form, setForm] = useState<PublicationFormState>(INITIAL_FORM_STATE)
  const [errors, setErrors] = useState<PublicationFormErrors>({})
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([])

  const updateField = useCallback(
    <Field extends keyof PublicationFormState>(
      field: Field,
      value: PublicationFormState[Field],
    ) => {
      setForm((currentForm) => ({
        ...currentForm,
        [field]: value,
      }))

      setErrors((currentErrors) => ({
        ...currentErrors,
        [field]: undefined,
        form: undefined,
      }))
    },
    [],
  )

  const toggleAmenity = useCallback((amenity: string) => {
    setSelectedAmenities((currentAmenities) =>
      currentAmenities.includes(amenity)
        ? currentAmenities.filter((currentAmenity) => currentAmenity !== amenity)
        : [...currentAmenities, amenity],
    )
  }, [])

  return {
    errors,
    form,
    selectedAmenities,
    setErrors,
    toggleAmenity,
    updateField,
  }
}
