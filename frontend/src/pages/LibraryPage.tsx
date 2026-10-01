import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { deleteLibraryEntry, updateLibraryEntry } from '../api'
import { PageHeader } from '../components/PageHeader'
import { Button } from '../components/ui/Button'
import { Drawer } from '../components/ui/Drawer'
import { EmptyState } from '../components/ui/EmptyState'
import { ErrorBox } from '../components/ui/ErrorBox'
import { GameCard, GameCardSkeleton, GameGrid } from '../components/ui/GameCard'
import { PlusIcon, TrashIcon } from '../components/ui/icons'
import { SearchField } from '../components/ui/SearchField'
import { Select } from '../components/ui/Select'
import { Skeleton } from '../components/ui/Skeleton'
import { StatTile } from '../components/ui/StatTile'
import { useLibrary } from '../library/LibraryContext'
import { useToast } from '../toast/ToastContext'
import { STATUSES, STATUS_LABELS } from '../types'
import type { LibraryEntryResponse, LibraryStatsResponse, Status, UpdateLibraryEntryRequest } from '../types'
import styles from './LibraryPage.module.css'

const RATINGS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

type SortKey = 'recent' | 'title' | 'rating'

// Sorting happens in the browser; the API always returns the newest entry first
function sortEntries(entries: LibraryEntryResponse[], sort: SortKey): LibraryEntryResponse[] {
  // Copy first: sort() changes the array it is called on
  const sorted = [...entries]
  if (sort === 'title') {
    sorted.sort((a, b) => a.title.localeCompare(b.title))
  } else if (sort === 'rating') {
    // Games without a rating count as 0, so they end up last
    sorted.sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0))
  }
  return sorted
}

function percentOf(count: number, total: number): string {
  return total === 0 ? '0% of library' : `${Math.round((count / total) * 100)}% of library`
}

export function LibraryPage() {
  const { entries, stats, loading, error, reload } = useLibrary()
  const { showToast } = useToast()
  const navigate = useNavigate()

  // '' means "all statuses"
  const [filter, setFilter] = useState<Status | ''>('')
  const [sort, setSort] = useState<SortKey>('recent')
  const [search, setSearch] = useState('')
  // The entry open in the edit drawer. Stored as an id, so the drawer always shows the freshest data.
  const [editingId, setEditingId] = useState<number | null>(null)

  const editing = entries.find((entry) => entry.id === editingId) ?? null
  const filtered = filter ? entries.filter((entry) => entry.status === filter) : entries
  const visible = sortEntries(filtered, sort)
  const ratedCount = entries.filter((entry) => entry.rating !== null).length

  // The header search field doesn't search here: it hands the text over to the Search page
  function handleSearch(event: FormEvent) {
    event.preventDefault()
    const query = search.trim()
    navigate(query ? `/search?q=${encodeURIComponent(query)}` : '/search')
  }

  async function handleSave(entry: LibraryEntryResponse, changes: UpdateLibraryEntryRequest) {
    try {
      await updateLibraryEntry(entry.id, changes)
      showToast(
        <>
          <strong>{entry.title}</strong> updated
        </>,
      )
      setEditingId(null)
      reload()
    } catch {
      showToast('Could not save the change.', 'error')
    }
  }

  async function handleRemove(entry: LibraryEntryResponse) {
    try {
      await deleteLibraryEntry(entry.id)
      showToast(
        <>
          <strong>{entry.title}</strong> removed from your library
        </>,
      )
      setEditingId(null)
      reload()
    } catch {
      showToast('Could not remove the game.', 'error')
    }
  }

  return (
    <>
      <PageHeader title="My library" subtitle="Everything you own, play, and plan to play.">
        <form className={styles.search} onSubmit={handleSearch} role="search">
          <SearchField
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search RAWG to add a game…"
            aria-label="Search games"
          />
        </form>
        <Button size="sm" className={styles.addButton} onClick={() => navigate('/search')}>
          <PlusIcon />
          Add game
        </Button>
      </PageHeader>

      {error && <ErrorBox>Could not load your library. Is the backend running?</ErrorBox>}

      {stats ? <StatTiles stats={stats} ratedCount={ratedCount} /> : loading && <StatTilesSkeleton />}

      <div className={styles.toolbar}>
        <div className={styles.filters}>
          <FilterPill label="All" count={stats?.total} active={filter === ''} onClick={() => setFilter('')} />
          {STATUSES.map((status) => (
            <FilterPill
              key={status}
              label={STATUS_LABELS[status]}
              count={stats ? countFor(stats, status) : undefined}
              active={filter === status}
              onClick={() => setFilter(status)}
            />
          ))}
        </div>
        <div className={styles.sort}>
          <Select value={sort} onChange={(e) => setSort(e.target.value as SortKey)} aria-label="Sort by">
            <option value="recent">Recently added</option>
            <option value="title">Title A–Z</option>
            <option value="rating">Rating high–low</option>
          </Select>
        </div>
      </div>

      {loading ? (
        <GameGrid>
          {Array.from({ length: 12 }, (_, i) => (
            <GameCardSkeleton key={i} />
          ))}
        </GameGrid>
      ) : entries.length === 0 ? (
        !error && (
          <EmptyState title="Your library is empty">
            <Link to="/search">Search for a game</Link> to add your first one.
          </EmptyState>
        )
      ) : visible.length === 0 ? (
        <EmptyState title="Nothing here">You have no games with this status.</EmptyState>
      ) : (
        <GameGrid>
          {visible.map((entry, index) => (
            <GameCard
              key={entry.id}
              index={index}
              title={entry.title}
              coverImageUrl={entry.coverImageUrl}
              releaseDate={entry.releaseDate}
              status={entry.status}
              rating={entry.rating}
              onOpen={() => setEditingId(entry.id)}
              actions={
                <>
                  <Button variant="light" size="sm" className={styles.editButton} onClick={() => setEditingId(entry.id)}>
                    Edit
                  </Button>
                  <button
                    type="button"
                    className={styles.removeButton}
                    onClick={() => handleRemove(entry)}
                    aria-label={`Remove ${entry.title} from library`}
                    title="Remove from library"
                  >
                    <TrashIcon />
                  </button>
                </>
              }
            />
          ))}
        </GameGrid>
      )}

      {editing && (
        <Drawer title={editing.title} onClose={() => setEditingId(null)}>
          {/* key: a different entry gets a fresh form instead of the previous entry's values */}
          <EditEntryForm
            key={editing.id}
            entry={editing}
            onSave={(changes) => handleSave(editing, changes)}
            onRemove={() => handleRemove(editing)}
          />
        </Drawer>
      )}
    </>
  )
}

function countFor(stats: LibraryStatsResponse, status: Status): number {
  if (status === 'PLAYING') return stats.playing
  if (status === 'BACKLOG') return stats.backlog
  return stats.completed
}

function StatTiles({ stats, ratedCount }: { stats: LibraryStatsResponse; ratedCount: number }) {
  return (
    <div className={styles.stats}>
      <StatTile
        label="Total"
        value={stats.total}
        hint={
          // One coloured segment per status; flex-grow makes each as wide as its share of the library
          <div className={styles.splitBar}>
            <div className={styles.splitPlaying} style={{ flexGrow: stats.playing }} />
            <div className={styles.splitBacklog} style={{ flexGrow: stats.backlog }} />
            <div className={styles.splitCompleted} style={{ flexGrow: stats.completed }} />
          </div>
        }
      />
      <StatTile
        label="Playing"
        status="PLAYING"
        value={stats.playing}
        hint={percentOf(stats.playing, stats.total)}
      />
      {/* The phone layout shows three tiles; Backlog and Completed are still in the filter pills there */}
      <div className={styles.desktopOnly}>
        <StatTile
          label="Backlog"
          status="BACKLOG"
          value={stats.backlog}
          hint={percentOf(stats.backlog, stats.total)}
        />
      </div>
      <div className={styles.desktopOnly}>
        <StatTile
          label="Completed"
          status="COMPLETED"
          value={stats.completed}
          hint={percentOf(stats.completed, stats.total)}
        />
      </div>
      <StatTile
        label="Average rating"
        value={
          stats.averageRating === null ? (
            <span className={styles.noRating}>Not rated yet</span>
          ) : (
            <>
              {stats.averageRating} <span className={styles.outOf}>/ 10</span>
            </>
          )
        }
        hint={`from ${ratedCount} rated ${ratedCount === 1 ? 'game' : 'games'}`}
      />
    </div>
  )
}

function StatTilesSkeleton() {
  return (
    <div className={styles.stats}>
      {Array.from({ length: 5 }, (_, i) => (
        <Skeleton key={i} className={styles.statSkeleton} />
      ))}
    </div>
  )
}

interface FilterPillProps {
  label: string
  // Left out while the stats are still loading
  count?: number
  active: boolean
  onClick: () => void
}

function FilterPill({ label, count, active, onClick }: FilterPillProps) {
  return (
    <button
      type="button"
      className={active ? `${styles.filter} ${styles.filterActive}` : styles.filter}
      onClick={onClick}
      aria-pressed={active}
    >
      {count === undefined ? label : `${label} · ${count}`}
    </button>
  )
}

interface EditEntryFormProps {
  entry: LibraryEntryResponse
  onSave: (changes: UpdateLibraryEntryRequest) => Promise<void>
  onRemove: () => void
}

// The content of the edit drawer. Nothing is sent until "Save changes" is clicked.
function EditEntryForm({ entry, onSave, onRemove }: EditEntryFormProps) {
  const [status, setStatus] = useState<Status>(entry.status)
  // A <select> works with strings; '' means "not rated"
  const [rating, setRating] = useState(entry.rating === null ? '' : String(entry.rating))
  const [notes, setNotes] = useState(entry.notes ?? '')
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const changes: UpdateLibraryEntryRequest = { status, notes }
    if (rating !== '') {
      changes.rating = Number(rating)
    }
    setSaving(true)
    await onSave(changes)
    setSaving(false)
  }

  return (
    <form className={styles.editForm} onSubmit={handleSubmit}>
      <Select label="Status" value={status} onChange={(e) => setStatus(e.target.value as Status)}>
        {STATUSES.map((option) => (
          <option key={option} value={option}>
            {STATUS_LABELS[option]}
          </option>
        ))}
      </Select>

      <Select label="Rating" value={rating} onChange={(e) => setRating(e.target.value)}>
        {/* The API can't clear a rating yet, so "not rated" can't be picked again once a rating is saved */}
        <option value="" disabled={entry.rating !== null}>
          Not rated
        </option>
        {RATINGS.map((option) => (
          <option key={option} value={option}>
            {option} / 10
          </option>
        ))}
      </Select>

      <label className={styles.notesField}>
        Notes
        <textarea
          className={styles.notes}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="What do you think of it so far?"
          rows={5}
        />
      </label>

      <Button type="submit" fullWidth loading={saving}>
        {saving ? 'Saving…' : 'Save changes'}
      </Button>
      <Button type="button" variant="danger" size="sm" fullWidth onClick={onRemove}>
        <TrashIcon />
        Remove from library
      </Button>
    </form>
  )
}
