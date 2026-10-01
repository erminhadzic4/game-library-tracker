import { createContext, useContext } from 'react'
import type { ReactNode } from 'react'

export type ToastKind = 'success' | 'error'

export interface ToastContextValue {
  // Shows a short message in the corner of the screen. It disappears by itself.
  showToast: (message: ReactNode, kind?: ToastKind) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast(): ToastContextValue {
  const value = useContext(ToastContext)
  if (!value) {
    throw new Error('useToast must be used inside <ToastProvider>')
  }
  return value
}
