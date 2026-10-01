import styles from './Skeleton.module.css'

// A grey pulsing block shown in place of content that is still loading.
// The caller gives it a size through className.
export function Skeleton({ className }: { className?: string }) {
  return <div className={className ? `${styles.skeleton} ${className}` : styles.skeleton} aria-hidden="true" />
}
