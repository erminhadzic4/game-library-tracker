import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ApiError, addLibraryEntry, searchGames } from '../api'
import { PageHeader } from '../components/PageHeader'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { ErrorBox } from '../components/ui/ErrorBox'
import { GameCard, GameCardSkeleton, GameGrid } from '../components/ui/GameCard'
import { CheckIcon, PlusIcon } from '../components/ui/icons'
import { SearchField } from '../components/ui/SearchField'
import { Select } from '../components/ui/Select'
import { useLibrary } from '../library/LibraryContext'
import { useToast } from '../toast/ToastContext'
import { STATUSES, STATUS_LABELS } from '../types'
import type { LibraryEntryResponse, RawgGame, Status } from '../types'
import styles from './SearchPage.module.css'

// How long to wait after the last keystroke before searching
const DEBOUNCE_MS = 300

export function SearchPage() {
  // The search text lives in the URL (?q=...) instead of in state, so a refresh or the Back button keeps it.
  // The Library page's search field links here with the same parameter.
  const [searchParams, setSearchParams] = useSearchParams()
  const query = searchParams.get('q') ?? ''
  // null until the first search has finished
  const [results, setResults] = useState<RawgGame[] | null>(null)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState(false)
  const { entries, reload } = useLibrary()

  const trimmed = query.trim()

  // replace: true swaps the current history entry, so Back doesn't step through every keystroke
  function handleQueryChange(value: string) {
    setSearchParams(value ? { q: value } : {}, { replace: true })
  }

  // Live search with a debounce: every change of the text starts a timer, and the cleanup cancels the previous one.
  // So the request is only sent once the user has stopped typing for 300 ms.
  useEffect(() => {
    if (!trimmed) return

    // Set by the cleanup, so a slow response for an older text can't overwrite a newer one
    let ignore = false
    const timer = window.setTimeout(() => {
      setSearching(true)
      searchGames(trimmed)
        .then((response) => {
          if (ignore) return
          setResults(response.results)
          setError(false)
          setSearching(false)
        })
        .catch(() => {
          if (ignore) return
          setError(true)
          setSearching(false)
        })
    }, DEBOUNCE_MS)

    return () => {
      ignore = true
      window.clearTimeout(timer)
    }
  }, [trimmed])

  // Finds the library entry for a search result, if the game was already added
  function libraryEntryFor(game: RawgGame): LibraryEntryResponse | undefined {
    return entries.find((entry) => entry.rawgId === game.id)
  }

  return (
    <>
      <PageHeader title="Search" subtitle="Find a game on RAWG and add it to your library." />

      <SearchField
        large
        value={query}
        onChange={(e) => handleQueryChange(e.target.value)}
        placeholder="Search for a game, e.g. The Witcher 3"
        aria-label="Game title"
        autoFocus
      />

      {!trimmed ? (
        <EmptyState title="Find your next game">Start typing a title and the results show up here.</EmptyState>
      ) : error ? (
        <ErrorBox>Search failed. Please try again.</ErrorBox>
      ) : searching || results === null ? (
        <GameGrid>
          {Array.from({ length: 12 }, (_, i) => (
            <GameCardSkeleton key={i} />
          ))}
        </GameGrid>
      ) : results.length === 0 ? (
        <EmptyState title="No games found">Try a different title or check the spelling.</EmptyState>
      ) : (
        <GameGrid>
          {results.map((game, index) => (
            <ResultCard key={game.id} game={game} index={index} libraryEntry={libraryEntryFor(game)} onAdded={reload} />
          ))}
        </GameGrid>
      )}
    </>
  )
}

interface ResultCardProps {
  game: RawgGame
  index: number
  // Set when the game is already in the user's library
  libraryEntry?: LibraryEntryResponse
  // Called after a successful add, so the library is fetched again
  onAdded: () => void
}

function ResultCard({ game, index, libraryEntry, onAdded }: ResultCardProps) {
  const { showToast } = useToast()
  const [status, setStatus] = useState<Status>('BACKLOG')
  const [adding, setAdding] = useState(false)

  async function handleAdd() {
    setAdding(true)
    try {
      // Sends the RAWG fields along, so the backend can cache the game without calling RAWG again
      await addLibraryEntry({
        rawgId: game.id,
        title: game.name,
        coverImageUrl: game.background_image,
        releaseDate: game.released,
        status,
      })
      showToast(
        <>
          <strong>{game.name}</strong> added to {STATUS_LABELS[status]}
        </>,
      )
      onAdded()
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        // Added in another tab, for example: reload so the card switches to "In library"
        showToast('That game is already in your library.', 'error')
        onAdded()
      } else {
        showToast('Could not add the game.', 'error')
      }
    } finally {
      setAdding(false)
    }
  }

  return (
    <GameCard
      index={index}
      title={game.name}
      coverImageUrl={game.background_image}
      releaseDate={game.released}
      status={libraryEntry?.status}
    >
      {libraryEntry ? (
        <p className={styles.inLibrary}>
          <CheckIcon size={16} />
          In library
        </p>
      ) : (
        <div className={styles.add}>
          <Select
            value={status}
            onChange={(e) => setStatus(e.target.value as Status)}
            aria-label={`Status for ${game.name}`}
          >
            {STATUSES.map((option) => (
              <option key={option} value={option}>
                {STATUS_LABELS[option]}
              </option>
            ))}
          </Select>
          <Button size="sm" loading={adding} onClick={handleAdd}>
            {!adding && <PlusIcon />}
            Add
          </Button>
        </div>
      )}
    </GameCard>
  )
}
