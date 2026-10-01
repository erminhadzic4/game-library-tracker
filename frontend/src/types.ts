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

export interface AddLibraryEntryRequest {
  rawgId: number
  title: string
  coverImageUrl: string | null
  releaseDate: string | null
  status: Status
}

// Partial update: a field that is left out stays unchanged on the server
export interface UpdateLibraryEntryRequest {
  status?: Status
  rating?: number
  notes?: string
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
