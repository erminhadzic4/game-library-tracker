import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError, register as registerRequest } from '../api'
import { useAuth } from '../auth/AuthContext'
import styles from './AuthForm.module.css'

export function RegisterPage() {
  const { token, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (token) {
    return <Navigate to="/library" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      // Register returns a token, so the user is logged in straight away
      const response = await registerRequest({ username, email, password })
      login(response.token)
      navigate('/library', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError('That username or email is already taken.')
      } else if (err instanceof ApiError && err.status === 400) {
        setError('Please fill in all fields.')
      } else {
        setError('Could not register. Is the backend running?')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.page}>
      <h1>Register</h1>
      <form className={styles.form} onSubmit={handleSubmit}>
        <label className={styles.field}>
          Username
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </label>
        <label className={styles.field}>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
        </label>
        <label className={styles.field}>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            required
          />
        </label>
        {error && <p className={styles.error}>{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Registering…' : 'Register'}
        </button>
      </form>
      <p className={styles.switch}>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </div>
  )
}
