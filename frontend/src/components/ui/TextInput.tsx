import { useId } from 'react'
import type { InputHTMLAttributes, ReactNode } from 'react'
import styles from './TextInput.module.css'

interface TextInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  // Shown before the text, inside the field
  icon?: ReactNode
  // Shown after the text, inside the field (PasswordInput puts its show/hide button here)
  trailing?: ReactNode
  // Red border, for a field whose value was rejected
  invalid?: boolean
}

// A labelled text field. The border lives on the wrapper, so the icons sit inside the field.
// Every other prop (value, onChange, type, required…) is passed straight to the <input>.
export function TextInput({ label, icon, trailing, invalid = false, id, ...rest }: TextInputProps) {
  // Links the <label> to the <input> without the caller having to invent an id
  const generatedId = useId()
  const inputId = id ?? generatedId

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={inputId}>
        {label}
      </label>
      <div className={invalid ? `${styles.control} ${styles.invalid}` : styles.control}>
        {icon && <span className={styles.icon}>{icon}</span>}
        <input id={inputId} className={styles.input} aria-invalid={invalid || undefined} {...rest} />
        {trailing}
      </div>
    </div>
  )
}
