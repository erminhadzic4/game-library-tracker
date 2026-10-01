import type { ReactNode } from 'react'
import type { Status } from '../../types'
import { StarIcon } from './icons'
import { Skeleton } from './Skeleton'
import { StatusPill } from './StatusPill'
import styles from './GameCard.module.css'

// Background colours for games without a cover image
const PLACEHOLDER_COLORS = [
  '#7A2230',
  '#6B4426',
  '#5E522A',
  '#233A62',
  '#552A78',
  '#2F6A3C',
  '#7A341C',
  '#225656',
  '#336A56',
  '#6C6418',
  '#2E4258',
  '#5A2548',
]

// Picks a colour from the title, so the same game always gets the same colour
function placeholderColor(title: string): string {
  let sum = 0
  for (let i = 0; i < title.length; i++) {
    sum += title.charCodeAt(i)
  }
  return PLACEHOLDER_COLORS[sum % PLACEHOLDER_COLORS.length]
}

// The big letter on the placeholder: the first letter of the title, skipping a leading "The"
function monogram(title: string): string {
  return title.replace(/^The /i, '').charAt(0).toUpperCase()
}

// The responsive grid that holds the cards: as many 240px+ columns as fit
export function GameGrid({ children }: { children: ReactNode }) {
  return <ul className={styles.grid}>{children}</ul>
}

interface GameCardProps {
  title: string
  coverImageUrl: string | null
  // ISO date (2015-05-18); only the year is shown
  releaseDate: string | null
  // Shown as a pill on the cover
  status?: Status
  // A number or null (not rated) shows the star; leave it out to show no rating at all
  rating?: number | null
  // Position in the grid, used to make the cards fade in one after another
  index?: number
  // Called when the cover is clicked or tapped
  onOpen?: () => void
  // Buttons shown over the cover on hover (mouse devices only)
  actions?: ReactNode
  // Extra content under the title, e.g. the "Add" controls on the Search page
  children?: ReactNode
}

// One game: its cover (or a coloured placeholder), title, year and rating
export function GameCard({
  title,
  coverImageUrl,
  releaseDate,
  status,
  rating,
  index = 0,
  onOpen,
  actions,
  children,
}: GameCardProps) {
  const year = releaseDate ? releaseDate.slice(0, 4) : 'TBA'
  // Each card starts 30 ms after the one before it. Capped, so a long list doesn't wait for seconds.
  const delay = `${Math.min(index, 12) * 30}ms`

  return (
    <li className={onOpen ? `${styles.card} ${styles.interactive}` : styles.card} style={{ animationDelay: delay }}>
      <div className={styles.cover}>
        {coverImageUrl ? (
          <img className={styles.image} src={coverImageUrl} alt="" loading="lazy" />
        ) : (
          <div
            className={styles.placeholder}
            style={{ background: `linear-gradient(165deg, ${placeholderColor(title)} 0%, #12131a 100%)` }}
          >
            <span className={styles.monogram}>{monogram(title)}</span>
          </div>
        )}
        {status && (
          <span className={styles.pill}>
            <StatusPill status={status} onCover />
          </span>
        )}
        {/* An invisible button over the whole cover: this is what a tap on a phone hits */}
        {onOpen && <button type="button" className={styles.open} onClick={onOpen} aria-label={`Edit ${title}`} />}
        {actions && <div className={styles.overlay}>{actions}</div>}
      </div>

      <div className={styles.text}>
        <span className={styles.title} title={title}>
          {title}
        </span>
        <span className={styles.meta}>
          {year}
          {rating !== undefined && (
            <>
              <span className={styles.separator} />
              <span className={rating === null ? styles.starEmpty : styles.star}>
                <StarIcon size={13} filled={rating !== null} />
              </span>
              {rating === null ? 'Not rated' : `${rating}/10`}
            </>
          )}
        </span>
      </div>

      {children}
    </li>
  )
}

// The loading version of a card: same shape, no content
export function GameCardSkeleton() {
  return (
    <li className={styles.skeletonCard}>
      <Skeleton className={styles.skeletonCover} />
      <Skeleton className={styles.skeletonTitle} />
      <Skeleton className={styles.skeletonMeta} />
    </li>
  )
}
