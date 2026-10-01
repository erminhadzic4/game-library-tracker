import type { ReactNode } from 'react'
import styles from './Card.module.css'

interface CardProps {
  children: ReactNode
  // Lets the caller add layout (padding, gap…) on top of the card's look
  className?: string
}

// A rounded panel with a border, used to group content
export function Card({ children, className }: CardProps) {
  return <div className={className ? `${styles.card} ${className}` : styles.card}>{children}</div>
}
