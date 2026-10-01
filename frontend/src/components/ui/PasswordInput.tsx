import { useState } from 'react'
import type { InputHTMLAttributes } from 'react'
import { EyeIcon, EyeOffIcon, LockIcon } from './icons'
import { TextInput } from './TextInput'
import styles from './TextInput.module.css'

interface PasswordInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string
  invalid?: boolean
}

// A TextInput with a lock icon and an eye button that switches between hidden and visible text
export function PasswordInput(props: PasswordInputProps) {
  const [visible, setVisible] = useState(false)

  return (
    <TextInput
      {...props}
      type={visible ? 'text' : 'password'}
      icon={<LockIcon />}
      trailing={
        // type="button" so that clicking it doesn't submit the form
        <button
          type="button"
          className={styles.toggle}
          onClick={() => setVisible(!visible)}
          aria-label={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? <EyeOffIcon /> : <EyeIcon />}
        </button>
      }
    />
  )
}
