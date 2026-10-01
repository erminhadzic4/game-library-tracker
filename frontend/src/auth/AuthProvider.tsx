import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { clearToken, getToken, saveToken, setUnauthorizedHandler } from '../api'
import { AuthContext } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  // Start from localStorage so a page refresh keeps the user logged in
  const [token, setToken] = useState<string | null>(getToken)

  const login = useCallback((newToken: string) => {
    saveToken(newToken)
    setToken(newToken)
  }, [])

  const logout = useCallback(() => {
    clearToken()
    setToken(null)
  }, [])

  // Lets api.ts log the user out when the backend answers 401 (expired or invalid token)
  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  const value = useMemo(() => ({ token, login, logout }), [token, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
