import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import { ErrorBoundary } from './components/ErrorBoundary'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { LibraryProvider } from './library/LibraryProvider'
import { LibraryPage } from './pages/LibraryPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { SearchPage } from './pages/SearchPage'
import { ToastProvider } from './toast/ToastProvider'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        {/* If a page crashes while rendering, this shows "Something went wrong" instead of a blank screen */}
        <ErrorBoundary>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* Everything below needs a token; ProtectedRoute redirects to /login without one */}
            <Route element={<ProtectedRoute />}>
              {/* The library data and the toasts are shared by the shell and both pages, so their providers wrap all three */}
              <Route
                element={
                  <LibraryProvider>
                    <ToastProvider>
                      <Layout />
                    </ToastProvider>
                  </LibraryProvider>
                }
              >
                <Route path="/library" element={<LibraryPage />} />
                <Route path="/search" element={<SearchPage />} />
              </Route>
            </Route>

            <Route path="*" element={<Navigate to="/library" replace />} />
          </Routes>
        </ErrorBoundary>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
