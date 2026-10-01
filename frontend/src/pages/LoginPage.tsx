import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError, login as loginRequest } from '../api'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'
import { Button } from '../components/ui/Button'
import { ErrorBox } from '../components/ui/ErrorBox'
import { ArrowRightIcon, UserIcon } from '../components/ui/icons'
import { PasswordInput } from '../components/ui/PasswordInput'
import { TextInput } from '../components/ui/TextInput'

export function LoginPage() {
  const { token, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  // True after a 401, so both fields get a red border (the backend doesn't say which one was wrong)
  const [wrongCredentials, setWrongCredentials] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  // Already logged in: nothing to do on this page
  if (token) {
    return <Navigate to="/library" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setWrongCredentials(false)
    setSubmitting(true)
    try {
      const response = await loginRequest({ username, password })
      login(response.token, username)
      navigate('/library', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError('Wrong username or password.')
        setWrongCredentials(true)
      } else {
        setError('Could not log in. Is the backend running?')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Log in to pick up where you left off."
      onSubmit={handleSubmit}
      footer={
        <>
          New here? <Link to="/register">Create an account</Link>
        </>
      }
    >
      <TextInput
        label="Username"
        icon={<UserIcon />}
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        autoComplete="username"
        invalid={wrongCredentials}
        required
      />
      <PasswordInput
        label="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        autoComplete="current-password"
        invalid={wrongCredentials}
        required
      />
      {error && <ErrorBox>{error}</ErrorBox>}
      <Button type="submit" fullWidth loading={submitting}>
        {submitting ? 'Logging in…' : 'Log in'}
        {!submitting && <ArrowRightIcon />}
      </Button>
    </AuthLayout>
  )
}
