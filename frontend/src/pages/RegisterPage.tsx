import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError, register as registerRequest } from '../api'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'
import { Button } from '../components/ui/Button'
import { ErrorBox } from '../components/ui/ErrorBox'
import { ArrowRightIcon, MailIcon, UserIcon } from '../components/ui/icons'
import { PasswordInput } from '../components/ui/PasswordInput'
import { TextInput } from '../components/ui/TextInput'

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
      login(response.token, username)
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
    <AuthLayout
      title="Create your account"
      subtitle="Start tracking your games in a minute."
      onSubmit={handleSubmit}
      footer={
        <>
          Already have an account? <Link to="/login">Log in</Link>
        </>
      }
    >
      <TextInput
        label="Username"
        icon={<UserIcon />}
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        autoComplete="username"
        required
      />
      <TextInput
        label="Email"
        type="email"
        icon={<MailIcon />}
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        autoComplete="email"
        required
      />
      <PasswordInput
        label="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        autoComplete="new-password"
        required
      />
      {error && <ErrorBox>{error}</ErrorBox>}
      <Button type="submit" fullWidth loading={submitting}>
        {submitting ? 'Registering…' : 'Create account'}
        {!submitting && <ArrowRightIcon />}
      </Button>
    </AuthLayout>
  )
}
