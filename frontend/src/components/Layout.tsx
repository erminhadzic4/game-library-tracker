import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import styles from './Layout.module.css'

// Shared page frame for the logged-in pages: navigation bar on top, the current page below
export function Layout() {
  const { logout } = useAuth()

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    isActive ? `${styles.link} ${styles.active}` : styles.link

  return (
    <>
      <header className={styles.header}>
        <span className={styles.brand}>Game Library Tracker</span>
        <nav className={styles.nav}>
          <NavLink to="/library" className={linkClass}>
            Library
          </NavLink>
          <NavLink to="/search" className={linkClass}>
            Search
          </NavLink>
        </nav>
        <button type="button" className={styles.logout} onClick={logout}>
          Log out
        </button>
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
    </>
  )
}
