import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { Button } from './ui/Button'
import styles from './ErrorBoundary.module.css'

interface ErrorBoundaryProps {
  children: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
}

// Catches an error thrown while rendering anything inside it, and shows a message instead of a blank screen.
// This is a class because React has no hook for it: only class components can be error boundaries.
// It does not catch errors in event handlers or failed requests; the pages handle those themselves.
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false }

  // React calls this when a child throws; the returned state makes render() show the message
  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true }
  }

  // Keeps the real error in the browser console for debugging
  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('The page crashed:', error, info.componentStack)
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children
    }

    return (
      <div className={styles.page} role="alert">
        <h1 className={styles.title}>Something went wrong</h1>
        <p>The page ran into a problem. Reloading usually fixes it.</p>
        <Button onClick={() => window.location.reload()}>Reload the page</Button>
      </div>
    )
  }
}
