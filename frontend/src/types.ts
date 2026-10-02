// These types mirror the backend DTO records (package com.erminhadzic.gamelibrarytracker.dto).
// Java LocalDate / LocalDateTime arrive as ISO strings in JSON.

export type Status = 'PLAYING' | 'BACKLOG' | 'COMPLETED'

export const STATUSES: Status[] = ['PLAYING', 'BACKLOG', 'COMPLETED']

export const STATUS_LABELS: Record<Status, string> = {
  PLAYING: 'Playing',
  BACKLOG: 'Backlog',
  COMPLETED: 'Completed',
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface AuthResponse {
  token: string
}

// One entry of the "errors" list in a validation error (400): which field broke a rule, and the message to show
export interface FieldError {
  field: string
  message: string
}

// The backend's error body (Spring's ProblemDetail, RFC 9457). Everything is optional because the body
// is parsed defensively: a response from the proxy, for example, has none of it.
export interface ProblemDetail {
  title?: string
  status?: number
  detail?: string
  errors?: FieldError[]
}

export interface AddLibraryEntryRequest {
  rawgId: number
  title: string
  coverImageUrl: string | null
  releaseDate: string | null
  status: Status
}

// Full replacement (PUT): all three fields are always sent. A null rating or notes clears the saved value.
export interface UpdateLibraryEntryRequest {
  status: Status
  rating: number | null
  notes: string | null
}

export interface LibraryEntryResponse {
  id: number
  gameId: number
  rawgId: number
  title: string
  coverImageUrl: string | null
  releaseDate: string | null
  status: Status
  rating: number | null
  notes: string | null
  addedAt: string
}

export interface LibraryStatsResponse {
  total: number
  playing: number
  backlog: number
  completed: number
  averageRating: number | null
}

// The backend passes RAWG's JSON through unchanged, so these are RAWG's field names.
// Only the fields the UI uses are listed.
export interface RawgGame {
  id: number
  name: string
  background_image: string | null
  released: string | null
}

export interface RawgSearchResponse {
  count: number
  results: RawgGame[]
}
