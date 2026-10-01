import type { InputHTMLAttributes } from 'react'
import { SearchIcon } from './icons'
import styles from './SearchField.module.css'

interface SearchFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  // The bigger version, used as the main input of the Search page
  large?: boolean
}

// A text field with a magnifier icon. It has no visible label, so pass aria-label.
export function SearchField({ large = false, ...rest }: SearchFieldProps) {
  return (
    <label className={large ? `${styles.field} ${styles.large}` : styles.field}>
      <SearchIcon size={large ? 22 : 18} />
      <input type="search" className={styles.input} {...rest} />
    </label>
  )
}
