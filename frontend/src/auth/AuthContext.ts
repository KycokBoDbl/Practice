import { createContext } from 'react'

import type {
  LoginRequest,
  ProfileResponse,
  RegisterRequest,
} from '../types/auth'

export interface AuthContextValue {
  profile: ProfileResponse | null
  loading: boolean
  isAuthenticated: boolean
  login: (request: LoginRequest) => Promise<ProfileResponse>
  register: (request: RegisterRequest) => Promise<ProfileResponse>
  refreshProfile: () => Promise<ProfileResponse>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

