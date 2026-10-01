import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { deleteLibraryEntry, getLibrary, getLibraryStats, updateLibraryEntry } from '../api'
import { STATUSES, STATUS_LABELS } from '../types'
import type { LibraryEntryResponse, LibraryStatsResponse, Status, UpdateLibraryEntryRequest } from '../types'
import styles from './LibraryPage.module.css'

const RATINGS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

export function LibraryPage() {
  const [entries, setEntries] = useState<LibraryEntryResponse[]>([])
  const [stats, setStats] = useState<LibraryStatsResponse | null>(null)
  // '' means "all statuses"
  const [filter, setFilter] = useState<Status | ''>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Increased after an update or delete, which makes the effect below fetch again
  const [reloadKey, setReloadKey] = useState(0)

  // Loads the list and the stats together, so the numbers always match what is shown
  useEffect(() => {
    // Set by the cleanup, so a slow response for an old filter can't overwrite a newer one
    let ignore = false

    Promise.all([getLibrary(filter || undefined), getLibraryStats()])
      .then(([entryList, statsResponse]) => {
        if (ignore) return
        setEntries(entryList)
        setStats(statsResponse)
        setError(null)
      })
      .catch(() => {
        if (!ignore) setError('Could not load your library.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [filter, reloadKey])

  async function handleUpdate(id: number, changes: UpdateLibraryEntryRequest) {
    try {
      await updateLibraryEntry(id, changes)
      setReloadKey((key) => key + 1)
    } catch {
      setError('Could not save the change.')
    }
  }

  async function handleDelete(id: number) {
    try {
      await deleteLibraryEntry(id)
      setReloadKey((key) => key + 1)
    } catch {
      setError('Could not remove the game.')
    }
  }

  return (
    <>
      <h1>My library</h1>

      {stats && (
        <ul className={styles.stats}>
          <li>Total: {stats.total}</li>
          <li>Playing: {stats.playing}</li>
          <li>Backlog: {stats.backlog}</li>
          <li>Completed: {stats.completed}</li>
          <li>Average rating: {stats.averageRating ?? '–'}</li>
        </ul>
      )}

      <label className={styles.filter}>
        Show
        <select value={filter} onChange={(e) => setFilter(e.target.value as Status | '')}>
          <option value="">All</option>
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {STATUS_LABELS[status]}
            </option>
          ))}
        </select>
      </label>

      {error && <p className={styles.error}>{error}</p>}
      {loading && <p>Loading…</p>}
      {!loading && !error && entries.length === 0 && (
        <p>
          No games here yet. <Link to="/search">Search for a game</Link> to add one.
        </p>
      )}

      <ul className={styles.list}>
        {entries.map((entry) => (
          <EntryCard key={entry.id} entry={entry} onUpdate={handleUpdate} onDelete={handleDelete} />
        ))}
      </ul>
    </>
  )
}

interface EntryCardProps {
  entry: LibraryEntryResponse
  onUpdate: (id: number, changes: UpdateLibraryEntryRequest) => void
  onDelete: (id: number) => void
}

function EntryCard({ entry, onUpdate, onDelete }: EntryCardProps) {
  // Notes are edited locally and only sent when the user clicks "Save notes"
  const [notes, setNotes] = useState(entry.notes ?? '')

  return (
    <li className={styles.card}>
      {entry.coverImageUrl ? (
        <img className={styles.cover} src={entry.coverImageUrl} alt="" />
      ) : (
        <div className={styles.cover} />
      )}
      <div className={styles.details}>
        <h2>{entry.title}</h2>
        <p className={styles.meta}>{entry.releaseDate ? `Released ${entry.releaseDate}` : 'Release date unknown'}</p>

        <div className={styles.controls}>
          <label>
            Status
            <select value={entry.status} onChange={(e) => onUpdate(entry.id, { status: e.target.value as Status })}>
              {STATUSES.map((status) => (
                <option key={status} value={status}>
                  {STATUS_LABELS[status]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Rating
            <select value={entry.rating ?? ''} onChange={(e) => onUpdate(entry.id, { rating: Number(e.target.value) })}>
              {/* The API can't clear a rating yet, so "not rated" can't be picked again once a rating is set */}
              <option value="" disabled>
                Not rated
              </option>
              {RATINGS.map((rating) => (
                <option key={rating} value={rating}>
                  {rating}
                </option>
              ))}
            </select>
          </label>
          <button type="button" className={styles.remove} onClick={() => onDelete(entry.id)}>
            Remove
          </button>
        </div>

        <textarea
          className={styles.notes}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Notes"
          rows={2}
        />
        <button type="button" disabled={notes === (entry.notes ?? '')} onClick={() => onUpdate(entry.id, { notes })}>
          Save notes
        </button>
      </div>
    </li>
  )
}
