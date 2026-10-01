import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { LogoutIcon } from './ui/icons'
import styles from './UserMenu.module.css'

// The round avatar in the page header on phones. Tapping it opens a small menu with the username and Log out.
// (On desktop the sidebar shows the same things, so PageHeader hides this.)
export function UserMenu() {
  const { username, logout } = useAuth()
  const [open, setOpen] = useState(false)

  return (
    <div className={styles.wrapper}>
      <button
        type="button"
        className={styles.avatar}
        onClick={() => setOpen(!open)}
        aria-label="Account menu"
        aria-expanded={open}
      >
        {username?.charAt(0).toUpperCase()}
      </button>

      {open && (
        <>
          {/* An invisible layer over the whole page: a tap anywhere outside the menu closes it */}
          <div className={styles.backdrop} onClick={() => setOpen(false)} />
          <div className={styles.menu}>
            <span className={styles.username}>{username}</span>
            <button type="button" className={styles.logout} onClick={logout}>
              <LogoutIcon />
              Log out
            </button>
          </div>
        </>
      )}
    </div>
  )
}
