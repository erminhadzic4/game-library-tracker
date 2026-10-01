import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { getLibrary, getLibraryStats } from '../api'
import type { LibraryEntryResponse, LibraryStatsResponse } from '../types'
import { LibraryContext } from './LibraryContext'

// Holds the user's library in one place, because three parts of the app need it:
// the Library page (the grid and the stat tiles), the Search page (which games are already added)
// and the sidebar (the game count).
export function LibraryProvider({ children }: { children: ReactNode }) {
  const [entries, setEntries] = useState<LibraryEntryResponse[]>([])
  const [stats, setStats] = useState<LibraryStatsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  // Increased by reload(), which makes the effect below fetch again
  const [reloadKey, setReloadKey] = useState(0)

  // Loads the list and the stats together, so the numbers always match what is shown
  useEffect(() => {
    // Set by the cleanup, so a slow response can't overwrite a newer one
    let ignore = false

    Promise.all([getLibrary(), getLibraryStats()])
      .then(([entryList, statsResponse]) => {
        if (ignore) return
        setEntries(entryList)
        setStats(statsResponse)
        setError(false)
      })
      .catch(() => {
        if (!ignore) setError(true)
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [reloadKey])

  const reload = useCallback(() => setReloadKey((key) => key + 1), [])

  const value = useMemo(() => ({ entries, stats, loading, error, reload }), [entries, stats, loading, error, reload])

  return <LibraryContext.Provider value={value}>{children}</LibraryContext.Provider>
}
