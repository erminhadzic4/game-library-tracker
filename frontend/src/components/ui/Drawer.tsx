import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { CloseIcon } from './icons'
import styles from './Drawer.module.css'

interface DrawerProps {
  title: string
  onClose: () => void
  children: ReactNode
}

// A panel that slides in over the page: from the right on desktop, from the bottom on phones.
// It is open for as long as it is rendered; the parent removes it when onClose is called.
// Esc, the X button and a click on the dark backdrop all close it.
export function Drawer({ title, onClose, children }: DrawerProps) {
  const panelRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  useEffect(() => {
    // Moves keyboard focus into the drawer, and stops the page behind it from scrolling
    panelRef.current?.focus()
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = ''
    }
  }, [])

  return (
    <>
      <div className={styles.backdrop} onClick={onClose} />
      <div ref={panelRef} className={styles.panel} role="dialog" aria-modal="true" aria-label={title} tabIndex={-1}>
        <header className={styles.header}>
          <h2 className={styles.title}>{title}</h2>
          <button type="button" className={styles.close} onClick={onClose} aria-label="Close">
            <CloseIcon />
          </button>
        </header>
        <div className={styles.body}>{children}</div>
      </div>
    </>
  )
}
