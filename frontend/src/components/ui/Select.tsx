import { useId } from 'react'
import type { SelectHTMLAttributes } from 'react'
import { ChevronDownIcon } from './icons'
import styles from './Select.module.css'

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  // Shown above the menu. Without it, pass aria-label so screen readers still get a name.
  label?: string
}

// A native <select> with the app's look. Native, so the keyboard and phone pickers work as usual.
// The options are passed as children, like with a normal <select>.
export function Select({ label, id, children, ...rest }: SelectProps) {
  const generatedId = useId()
  const selectId = id ?? generatedId

  return (
    <div className={styles.field}>
      {label && (
        <label className={styles.label} htmlFor={selectId}>
          {label}
        </label>
      )}
      <div className={styles.control}>
        <select id={selectId} className={styles.select} {...rest}>
          {children}
        </select>
        <span className={styles.chevron}>
          <ChevronDownIcon size={16} />
        </span>
      </div>
    </div>
  )
}
