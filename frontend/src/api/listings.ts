import { api } from './client'
import type { Listing } from '../types/listing'

export async function getListings(): Promise<Listing[]> {
  const response = await api.get<Listing[]>('/api/listings')
  return response.data
}