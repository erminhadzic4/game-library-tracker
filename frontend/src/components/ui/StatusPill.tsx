import { STATUS_LABELS } from '../../types'
import type { Status } from '../../types'
import styles from './StatusPill.module.css'

// One colour per status: blue = playing, amber = backlog, green = completed
const STATUS_CLASSES: Record<Status, string> = {
  PLAYING: styles.playing,
  BACKLOG: styles.backlog,
  COMPLETED: styles.completed,
}

interface StatusPillProps {
  status: Status
  // The smaller, more solid version that stays readable on top of a game cover
  onCover?: boolean
}

// A small rounded label showing a library status in its colour
export function StatusPill({ status, onCover = false }: StatusPillProps) {
  const classes = [styles.pill, STATUS_CLASSES[status]]
  if (onCover) classes.push(styles.onCover)

  return <span className={classes.join(' ')}>{STATUS_LABELS[status]}</span>
}
