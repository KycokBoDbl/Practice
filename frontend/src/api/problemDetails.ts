import axios from 'axios'

import type { ProblemDetail } from '../types/auth'

export interface ParsedProblemDetail {
  status?: number
  message: string
  fieldErrors: Record<string, string>
  problem?: ProblemDetail
}

const DEFAULT_ERROR_MESSAGE = 'Не удалось выполнить запрос. Попробуйте позже.'

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  return isObject(value) && (
    typeof value.status === 'number' ||
    typeof value.detail === 'string' ||
    Array.isArray(value.errors)
  )
}

function getFieldErrors(problem: ProblemDetail) {
  const errors: Record<string, string> = {}

  for (const error of problem.errors ?? []) {
    if (typeof error.field === 'string' && typeof error.message === 'string') {
      errors[error.field] = error.message
    }
  }

  return errors
}

export function parseProblemDetail(
  value: unknown,
  fallbackMessage = DEFAULT_ERROR_MESSAGE,
): ParsedProblemDetail {
  if (!isProblemDetail(value)) {
    return {
      message: fallbackMessage,
      fieldErrors: {},
    }
  }

  return {
    status: value.status,
    message: value.detail || value.title || fallbackMessage,
    fieldErrors: getFieldErrors(value),
    problem: value,
  }
}

export function parseApiError(
  error: unknown,
  fallbackMessage = DEFAULT_ERROR_MESSAGE,
): ParsedProblemDetail {
  if (axios.isAxiosError(error)) {
    return parseProblemDetail(error.response?.data, fallbackMessage)
  }

  return parseProblemDetail(error, fallbackMessage)
}

