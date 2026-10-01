import type { ReactNode } from 'react'
import { AlertIcon, CheckIcon } from './icons'
import styles from './Toast.module.css'

// The fixed corner of the screen where toasts appear.
// role="status" makes screen readers read a new toast without interrupting the user.
export function ToastStack({ children }: { children: ReactNode }) {
  return (
    <div className={styles.stack} role="status" aria-live="polite">
      {children}
    </div>
  )
}

interface ToastProps {
  kind: 'success' | 'error'
  children: ReactNode
}

// One message: a green tick for success, a red mark for an error
export function Toast({ kind, children }: ToastProps) {
  return (
    <div className={styles.toast}>
      <span className={`${styles.icon} ${styles[kind]}`}>
        {kind === 'success' ? <CheckIcon size={16} /> : <AlertIcon size={16} />}
      </span>
      <span>{children}</span>
    </div>
  )
}
