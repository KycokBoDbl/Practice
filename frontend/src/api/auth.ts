import { api } from './client'
import type {
  LoginRequest,
  ProfileResponse,
  RegisterRequest,
  TokenResponse,
} from '../types/auth'

export async function register(request: RegisterRequest): Promise<ProfileResponse> {
  const response = await api.post<ProfileResponse>('/api/auth/register', request)
  return response.data
}

export async function login(request: LoginRequest): Promise<TokenResponse> {
  const response = await api.post<TokenResponse>('/api/auth/login', request)
  return response.data
}

export async function getMe(): Promise<ProfileResponse> {
  const response = await api.get<ProfileResponse>('/api/auth/me')
  return response.data
}

