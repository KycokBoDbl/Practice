export type UserRole = 'LANDLORD' | 'TENANT'

export interface RegisterRequest {
  role: UserRole
  legalName: string
  taxId: string
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export interface ProfileResponse {
  userId: number
  organizationId: number
  role: UserRole
  legalName: string
  taxId: string
  email: string
}

export interface ProblemDetailFieldError {
  field: string
  message: string
}

export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: ProblemDetailFieldError[]
  properties?: Record<string, unknown>
}

