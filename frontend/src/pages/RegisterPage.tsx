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
  // Field names from the backend's validation errors ("username", "email", "password")
  const [invalidFields, setInvalidFields] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)

  if (token) {
    return <Navigate to="/library" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setInvalidFields([])
    setSubmitting(true)
    try {
      // Register returns a token, so the user is logged in straight away
      const response = await registerRequest({ username, email, password })
      login(response.token, username)
      navigate('/library', { replace: true })
    } catch (err) {
      // The backend's own message is used when there is one; the fixed texts are the fallback
      if (err instanceof ApiError && err.status === 409) {
        // "Username is already taken" or "Email is already registered"
        setError(err.detail ? `${err.detail}.` : 'That username or email is already taken.')
      } else if (err instanceof ApiError && err.status === 400) {
        if (err.fieldErrors.length > 0) {
          // One sentence per broken rule, and a red border on each field that is named
          setError(err.fieldErrors.map((fieldError) => `${fieldError.message}.`).join(' '))
          setInvalidFields(err.fieldErrors.map((fieldError) => fieldError.field))
        } else {
          setError('Please fill in all fields.')
        }
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
        invalid={invalidFields.includes('username')}
        required
      />
      <TextInput
        label="Email"
        type="email"
        icon={<MailIcon />}
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        autoComplete="email"
        invalid={invalidFields.includes('email')}
        required
      />
      <PasswordInput
        label="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        autoComplete="new-password"
        invalid={invalidFields.includes('password')}
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
