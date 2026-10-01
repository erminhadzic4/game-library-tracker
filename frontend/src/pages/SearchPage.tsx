import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, addLibraryEntry, searchGames } from '../api'
import { STATUSES, STATUS_LABELS } from '../types'
import type { RawgGame, Status } from '../types'
import styles from './SearchPage.module.css'

export function SearchPage() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<RawgGame[] | null>(null)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSearch(event: FormEvent) {
    event.preventDefault()
    if (!query.trim()) {
      return
    }
    setError(null)
    setSearching(true)
    try {
      const response = await searchGames(query.trim())
      setResults(response.results)
    } catch {
      setError('Search failed. Please try again.')
    } finally {
      setSearching(false)
    }
  }

  return (
    <>
      <h1>Search games</h1>
      <form className={styles.form} onSubmit={handleSearch}>
        <input
          className={styles.query}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="e.g. The Witcher 3"
          aria-label="Game title"
        />
        <button type="submit" disabled={searching}>
          {searching ? 'Searching…' : 'Search'}
        </button>
      </form>

      {error && <p className={styles.error}>{error}</p>}
      {results && results.length === 0 && <p>No games found.</p>}

      <ul className={styles.grid}>
        {results?.map((game) => (
          <ResultCard key={game.id} game={game} />
        ))}
      </ul>
    </>
  )
}

function ResultCard({ game }: { game: RawgGame }) {
  const [status, setStatus] = useState<Status>('BACKLOG')
  // Message shown instead of the Add button once the game is added (or was already in the library)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleAdd() {
    setError(null)
    try {
      // Sends the RAWG fields along, so the backend can cache the game without calling RAWG again
      await addLibraryEntry({
        rawgId: game.id,
        title: game.name,
        coverImageUrl: game.background_image,
        releaseDate: game.released,
        status,
      })
      setMessage('Added to library')
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setMessage('Already in your library')
      } else {
        setError('Could not add the game.')
      }
    }
  }

  return (
    <li className={styles.card}>
      {game.background_image ? (
        <img className={styles.cover} src={game.background_image} alt="" />
      ) : (
        <div className={styles.cover} />
      )}
      <div className={styles.body}>
        <h2>{game.name}</h2>
        <p className={styles.meta}>{game.released ?? 'Release date unknown'}</p>
        {message ? (
          <p className={styles.added}>{message}</p>
        ) : (
          <div className={styles.actions}>
            <select value={status} onChange={(e) => setStatus(e.target.value as Status)} aria-label="Status">
              {STATUSES.map((option) => (
                <option key={option} value={option}>
                  {STATUS_LABELS[option]}
                </option>
              ))}
            </select>
            <button type="button" onClick={handleAdd}>
              Add
            </button>
          </div>
        )}
        {error && <p className={styles.error}>{error}</p>}
      </div>
    </li>
  )
}
