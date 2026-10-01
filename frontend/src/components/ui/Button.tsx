import type { ButtonHTMLAttributes } from 'react'
import styles from './Button.module.css'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  // primary: filled with the accent colour. secondary: outlined, for less important actions.
  variant?: 'primary' | 'secondary'
  fullWidth?: boolean
  // Shows a spinner and disables the button, for while a request is running
  loading?: boolean
}

// A normal <button> with the app's look. Every other prop (type, onClick…) is passed straight through.
export function Button({
  variant = 'primary',
  fullWidth = false,
  loading = false,
  disabled,
  className,
  children,
  ...rest
}: ButtonProps) {
  const classes = [styles.button, styles[variant]]
  if (fullWidth) classes.push(styles.fullWidth)
  if (className) classes.push(className)

  return (
    <button className={classes.join(' ')} disabled={disabled || loading} {...rest}>
      {loading && <span className={styles.spinner} aria-hidden="true" />}
      {children}
    </button>
  )
}
