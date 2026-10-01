import type { ButtonHTMLAttributes } from 'react'
import styles from './Button.module.css'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  // primary: filled with the accent colour. secondary: outlined, for less important actions.
  // light: white, for use on top of a game cover. danger: outlined in red, for removing things.
  variant?: 'primary' | 'secondary' | 'light' | 'danger'
  // md is the big form button; sm fits toolbars and cards
  size?: 'md' | 'sm'
  fullWidth?: boolean
  // Shows a spinner and disables the button, for while a request is running
  loading?: boolean
}

// A normal <button> with the app's look. Every other prop (type, onClick…) is passed straight through.
export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  disabled,
  className,
  children,
  ...rest
}: ButtonProps) {
  const classes = [styles.button, styles[variant], styles[size]]
  if (fullWidth) classes.push(styles.fullWidth)
  if (className) classes.push(className)

  return (
    <button className={classes.join(' ')} disabled={disabled || loading} {...rest}>
      {loading && <span className={styles.spinner} aria-hidden="true" />}
      {children}
    </button>
  )
}
