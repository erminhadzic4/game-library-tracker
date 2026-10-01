import type { ReactNode } from 'react'
import { AlertIcon } from './icons'
import styles from './ErrorBox.module.css'

// An error message in a red box. role="alert" makes screen readers announce it when it appears.
export function ErrorBox({ children }: { children: ReactNode }) {
  return (
    <div role="alert" className={styles.box}>
      <AlertIcon />
      <span>{children}</span>
    </div>
  )
}
