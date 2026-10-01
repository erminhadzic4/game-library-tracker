import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ApiError, addLibraryEntry, discoverGames, searchGames } from '../api'
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

// RAWG's lists contain entries without a picture and the same game more than once.
// The discover lists are a showcase, so those are left out here. Search results are not filtered:
// someone searching for a game should find it even if it has no picture.
function cleanDiscoverList(games: RawgGame[]): RawgGame[] {
  const seenNames = new Set<string>()
  return games.filter((game) => {
    if (!game.background_image) return false
    // Compared in lower case, so "Portal" and "PORTAL" count as the same name. The first one is kept.
    const name = game.name.trim().toLowerCase()
    if (seenNames.has(name)) return false
    seenNames.add(name)
    return true
  })
}

export function SearchPage() {
  // The search text lives in the URL (?q=...) instead of in state, so a refresh or the Back button keeps it.
  // The Library page's search field links here with the same parameter.
  const [searchParams, setSearchParams] = useSearchParams()
  const query = searchParams.get('q') ?? ''
  // null until the first search has finished
  const [results, setResults] = useState<RawgGame[] | null>(null)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState(false)

  // The two lists shown while the search field is empty. null until they have loaded.
  const [popular, setPopular] = useState<RawgGame[] | null>(null)
  const [topRated, setTopRated] = useState<RawgGame[] | null>(null)
  const [discoverError, setDiscoverError] = useState(false)

  const trimmed = query.trim()

  // Loads the discover lists once, when the page opens. The empty dependency list means typing never refetches them:
  // they stay in state, and are simply hidden while there is search text.
  useEffect(() => {
    let ignore = false

    Promise.all([discoverGames('popular'), discoverGames('top')])
      .then(([popularResponse, topResponse]) => {
        if (ignore) return
        setPopular(cleanDiscoverList(popularResponse.results))
        setTopRated(cleanDiscoverList(topResponse.results))
      })
      .catch(() => {
        if (!ignore) setDiscoverError(true)
      })

    return () => {
      ignore = true
    }
  }, [])

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
        // Nothing typed: show the discover lists instead of an empty page
        discoverError ? (
          <ErrorBox>Could not load the suggestions. You can still search above.</ErrorBox>
        ) : (
          <>
            <section className={styles.section}>
              <h2 className={styles.sectionTitle}>Popular right now</h2>
              <GameList games={popular} />
            </section>
            <section className={styles.section}>
              <h2 className={styles.sectionTitle}>All-time greats</h2>
              <GameList games={topRated} />
            </section>
          </>
        )
      ) : error ? (
        <ErrorBox>Search failed. Please try again.</ErrorBox>
      ) : results !== null && results.length === 0 && !searching ? (
        <EmptyState title="No games found">Try a different title or check the spelling.</EmptyState>
      ) : (
        <GameList games={searching ? null : results} />
      )}
    </>
  )
}

// A grid of result cards, or skeleton cards while the games are still loading (games = null).
// Used for the search results and for both discover lists.
function GameList({ games }: { games: RawgGame[] | null }) {
  const { entries, reload } = useLibrary()

  if (games === null) {
    return (
      <GameGrid>
        {Array.from({ length: 12 }, (_, i) => (
          <GameCardSkeleton key={i} />
        ))}
      </GameGrid>
    )
  }

  return (
    <GameGrid>
      {games.map((game, index) => (
        <ResultCard
          key={game.id}
          game={game}
          index={index}
          // The library entry for this game, if it was already added
          libraryEntry={entries.find((entry) => entry.rawgId === game.id)}
          onAdded={reload}
        />
      ))}
    </GameGrid>
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
