import type { SpaceType } from './spaceType'

export interface Listing {
  id: number
  title: string
  description: string
  city: string
  address: string
  pricePerHour: number
  capacity: number
  spaceType: SpaceType
  imageUrl: string
}
