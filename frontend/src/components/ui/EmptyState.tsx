import type { ReactNode } from 'react'
import styles from './EmptyState.module.css'

interface EmptyStateProps {
  title: string
  // The explanation under the title; may contain a link
  children?: ReactNode
}

// Shown where a list would be, when there is nothing to list
export function EmptyState({ title, children }: EmptyStateProps) {
  return (
    <div className={styles.empty}>
      <h2 className={styles.title}>{title}</h2>
      {children && <p>{children}</p>}
    </div>
  )
}
