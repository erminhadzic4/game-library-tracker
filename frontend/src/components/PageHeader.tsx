import type { ReactNode } from 'react'
import { UserMenu } from './UserMenu'
import styles from './PageHeader.module.css'

interface PageHeaderProps {
  title: string
  subtitle?: string
  // Controls on the right of the title (the Library page puts its search field and Add button here)
  children?: ReactNode
}

// The top of a logged-in page: the title, and on phones the avatar menu next to it
export function PageHeader({ title, subtitle, children }: PageHeaderProps) {
  return (
    <header className={styles.header}>
      <div className={styles.titles}>
        <h1 className={styles.title}>{title}</h1>
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
      </div>
      <div className={styles.userMenu}>
        <UserMenu />
      </div>
      {children && <div className={styles.actions}>{children}</div>}
    </header>
  )
}
