import { createContext, useContext } from 'react'
import type { LibraryEntryResponse, LibraryStatsResponse } from '../types'

export interface LibraryContextValue {
  // Every entry of the logged-in user, newest first (the order the API returns)
  entries: LibraryEntryResponse[]
  stats: LibraryStatsResponse | null
  // True only until the first load finishes
  loading: boolean
  error: boolean
  // Fetches the entries and the stats again, after something was added, changed or removed
  reload: () => void
}

export const LibraryContext = createContext<LibraryContextValue | null>(null)

export function useLibrary(): LibraryContextValue {
  const value = useContext(LibraryContext)
  if (!value) {
    throw new Error('useLibrary must be used inside <LibraryProvider>')
  }
  return value
}
