import { STATUS_LABELS } from '../../types'
import type { Status } from '../../types'
import styles from './StatusPill.module.css'

// One colour per status: blue = playing, amber = backlog, green = completed
const STATUS_CLASSES: Record<Status, string> = {
  PLAYING: styles.playing,
  BACKLOG: styles.backlog,
  COMPLETED: styles.completed,
}

// A small rounded label showing a library status in its colour
export function StatusPill({ status }: { status: Status }) {
  return <span className={`${styles.pill} ${STATUS_CLASSES[status]}`}>{STATUS_LABELS[status]}</span>
}
