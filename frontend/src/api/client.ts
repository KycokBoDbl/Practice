import axios from 'axios'

import { clearStoredAuthToken, getAuthorizationHeader } from './authTokenStorage'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://127.0.0.1:8081',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const authorization = getAuthorizationHeader()

  if (authorization) {
    config.headers.Authorization = authorization
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearStoredAuthToken()
    }

    return Promise.reject(error)
  },
)
