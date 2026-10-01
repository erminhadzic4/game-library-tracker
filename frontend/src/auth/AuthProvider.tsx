import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { clearSession, getToken, getUsername, saveSession, setUnauthorizedHandler } from '../api'
import { AuthContext } from './AuthContext'

interface Session {
  token: string | null
  username: string | null
}

const LOGGED_OUT: Session = { token: null, username: null }

// Reads the saved session from localStorage, so a page refresh keeps the user logged in
function readSession(): Session {
  const token = getToken()
  const username = getUsername()
  // A token without a username is a session saved before usernames were stored: log out, so the user logs in again
  if (!token || !username) {
    clearSession()
    return LOGGED_OUT
  }
  return { token, username }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session>(readSession)

  const login = useCallback((token: string, username: string) => {
    saveSession(token, username)
    setSession({ token, username })
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setSession(LOGGED_OUT)
  }, [])

  // Lets api.ts log the user out when the backend answers 401 (expired or invalid token)
  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  const value = useMemo(
    () => ({ token: session.token, username: session.username, login, logout }),
    [session, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
