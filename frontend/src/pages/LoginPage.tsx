import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError, login as loginRequest } from '../api'
import { useAuth } from '../auth/AuthContext'
import styles from './AuthForm.module.css'

export function LoginPage() {
  const { token, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // Already logged in: nothing to do on this page
  if (token) {
    return <Navigate to="/library" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const response = await loginRequest({ username, password })
      login(response.token)
      navigate('/library', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError('Wrong username or password.')
      } else {
        setError('Could not log in. Is the backend running?')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.page}>
      <h1>Log in</h1>
      <form className={styles.form} onSubmit={handleSubmit}>
        <label className={styles.field}>
          Username
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </label>
        <label className={styles.field}>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error && <p className={styles.error}>{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className={styles.switch}>
        No account yet? <Link to="/register">Register</Link>
      </p>
    </div>
  )
}
