import { useCallback, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Toast, ToastStack } from '../components/ui/Toast'
import { ToastContext } from './ToastContext'
import type { ToastKind } from './ToastContext'

// How long a toast stays on screen
const TOAST_DURATION_MS = 3500

interface ToastItem {
  id: number
  message: ReactNode
  kind: ToastKind
}

// Keeps the list of visible toasts and draws them on top of the page.
// Any component below it can call useToast().showToast(...).
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  // Gives every toast its own id, so the right one is removed when its time is up
  const nextId = useRef(1)

  const showToast = useCallback((message: ReactNode, kind: ToastKind = 'success') => {
    const id = nextId.current++
    setToasts((current) => [...current, { id, message, kind }])
    window.setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id))
    }, TOAST_DURATION_MS)
  }, [])

  const value = useMemo(() => ({ showToast }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastStack>
        {toasts.map((toast) => (
          <Toast key={toast.id} kind={toast.kind}>
            {toast.message}
          </Toast>
        ))}
      </ToastStack>
    </ToastContext.Provider>
  )
}
