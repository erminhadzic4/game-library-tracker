import type { FormEvent, ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { STATUSES } from '../types'
import { Card } from './ui/Card'
import { LogoIcon } from './ui/icons'
import { StatusPill } from './ui/StatusPill'
import styles from './AuthLayout.module.css'

// Decorative background: a wall of coloured tiles standing in for game covers
const TILE_GAMES = [
  { title: 'The Witcher 3', color: '#5A3A22' },
  { title: 'Hades', color: '#6B1F28' },
  { title: 'Elden Ring', color: '#4F4526' },
  { title: 'Hollow Knight', color: '#1F3150' },
  { title: 'Celeste', color: '#4A2468' },
  { title: 'Stardew Valley', color: '#2C5A36' },
  { title: 'Red Dead 2', color: '#6A2E1A' },
  { title: 'Disco Elysium', color: '#1F4A4A' },
  { title: 'Breath of the Wild', color: '#2F5A4A' },
  { title: 'Cyberpunk 2077', color: '#5E5718' },
  { title: 'Portal 2', color: '#2A3A4C' },
  { title: "Baldur's Gate 3", color: '#4E2240' },
]

// 40 tiles fill the wall. Stepping through the list by 5 keeps the same game from sitting next to itself.
const TILES = Array.from({ length: 40 }, (_, i) => TILE_GAMES[(i * 5) % TILE_GAMES.length])

interface AuthLayoutProps {
  title: string
  subtitle: string
  onSubmit: (event: FormEvent) => void
  // The fields, the error box and the submit button
  children: ReactNode
  // The line under the form that links to the other auth page
  footer: ReactNode
}

// Shared frame for the Login and Register pages: the intro text on the left, the form card on the right
export function AuthLayout({ title, subtitle, onSubmit, children, footer }: AuthLayoutProps) {
  const tabClass = ({ isActive }: { isActive: boolean }) => (isActive ? `${styles.tab} ${styles.tabActive}` : styles.tab)

  return (
    <div className={styles.page}>
      <div className={styles.tiles} aria-hidden="true">
        {TILES.map((tile, i) => (
          <div key={i} className={styles.tile} style={{ background: tile.color }}>
            {tile.title}
          </div>
        ))}
      </div>
      <div className={styles.overlay} />

      <div className={styles.content}>
        <section className={styles.intro}>
          <div className={styles.brand}>
            <span className={styles.logo}>
              <LogoIcon size={22} />
            </span>
            Game Library
          </div>
          <h1 className={styles.headline}>Track every game you play.</h1>
          <p className={styles.lede}>
            Search the RAWG catalogue, build your library, and keep tabs on what you're playing, what's waiting, and
            what you've finished.
          </p>
          <div className={styles.pills}>
            {STATUSES.map((status) => (
              <StatusPill key={status} status={status} />
            ))}
          </div>
        </section>

        <Card className={styles.formCard}>
          <nav className={styles.tabs}>
            <NavLink to="/login" className={tabClass}>
              Log in
            </NavLink>
            <NavLink to="/register" className={tabClass}>
              Register
            </NavLink>
          </nav>
          <div>
            <h2 className={styles.title}>{title}</h2>
            <p className={styles.subtitle}>{subtitle}</p>
          </div>
          <form className={styles.form} onSubmit={onSubmit}>
            {children}
          </form>
          <p className={styles.footer}>{footer}</p>
        </Card>
      </div>
    </div>
  )
}
