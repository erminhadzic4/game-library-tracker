import type { ReactNode } from 'react'
import type { Status } from '../../types'
import styles from './StatTile.module.css'

const DOT_CLASSES: Record<Status, string> = {
  PLAYING: styles.playing,
  BACKLOG: styles.backlog,
  COMPLETED: styles.completed,
}

interface StatTileProps {
  label: string
  value: ReactNode
  // Puts a dot in that status's colour before the label
  status?: Status
  // The small line under the number (hidden on phones, where the tiles are compact)
  hint?: ReactNode
}

// One number from the library stats, in a box
export function StatTile({ label, value, status, hint }: StatTileProps) {
  return (
    <div className={styles.tile}>
      <span className={styles.label}>
        {status && <span className={`${styles.dot} ${DOT_CLASSES[status]}`} />}
        {label}
      </span>
      <span className={styles.value}>{value}</span>
      {hint && <div className={styles.hint}>{hint}</div>}
    </div>
  )
}
