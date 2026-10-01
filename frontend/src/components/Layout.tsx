import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useLibrary } from '../library/LibraryContext'
import { GridIcon, LogoIcon, LogoutIcon, SearchIcon } from './ui/icons'
import styles from './Layout.module.css'

// The app shell for the logged-in pages.
// Desktop: a sidebar on the left (logo, navigation, the user block). Phones: a tab bar at the bottom instead.
// Both are always rendered; the CSS shows the one that fits the screen.
export function Layout() {
  const { username, logout } = useAuth()
  const { stats } = useLibrary()

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    isActive ? `${styles.link} ${styles.linkActive}` : styles.link
  const tabClass = ({ isActive }: { isActive: boolean }) => (isActive ? `${styles.tab} ${styles.tabActive}` : styles.tab)

  // Empty until the stats have loaded
  const gameCount = stats ? `${stats.total} ${stats.total === 1 ? 'game' : 'games'}` : ''

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        {/* The aside is the full-height coloured column; this inner box is the part that stays in view */}
        <div className={styles.sidebarContent}>
          <div className={styles.brand}>
            <span className={styles.logo}>
              <LogoIcon size={20} />
            </span>
            Game Library
          </div>

          <nav className={styles.nav}>
            <NavLink to="/library" className={linkClass}>
              <GridIcon size={20} />
              Library
            </NavLink>
            <NavLink to="/search" className={linkClass}>
              <SearchIcon size={20} />
              Search
            </NavLink>
          </nav>

          <div className={styles.user}>
            <span className={styles.avatar}>{username?.charAt(0).toUpperCase()}</span>
            <div className={styles.userText}>
              <span className={styles.username}>{username}</span>
              <span className={styles.gameCount}>{gameCount}</span>
            </div>
            <button type="button" className={styles.logout} onClick={logout} aria-label="Log out" title="Log out">
              <LogoutIcon />
            </button>
          </div>
        </div>
      </aside>

      <main className={styles.main}>
        <Outlet />
      </main>

      <nav className={styles.tabBar}>
        <NavLink to="/library" className={tabClass}>
          <GridIcon size={22} />
          Library
        </NavLink>
        <NavLink to="/search" className={tabClass}>
          <SearchIcon size={22} />
          Search
        </NavLink>
      </nav>
    </div>
  )
}
