import type {
  AddLibraryEntryRequest,
  AuthResponse,
  LibraryEntryResponse,
  LibraryStatsResponse,
  LoginRequest,
  RawgSearchResponse,
  RegisterRequest,
  Status,
  UpdateLibraryEntryRequest,
} from './types'

const TOKEN_KEY = 'token'
const USERNAME_KEY = 'username'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

// The backend only returns a token, so the username typed at login is saved next to it for the UI to show
export function getUsername(): string | null {
  return localStorage.getItem(USERNAME_KEY)
}

export function saveSession(token: string, username: string) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USERNAME_KEY, username)
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
}

// Thrown for any non-2xx response, so pages can show a message based on the status code
export class ApiError extends Error {
  status: number

  constructor(status: number) {
    super(`Request failed with status ${status}`)
    this.status = status
  }
}

// AuthProvider registers its logout function here, so a 401 from any request logs the user out
let onUnauthorized: () => void = () => {}

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers = new Headers(options.headers)
  if (options.body) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  // Paths start with /api, which the Vite dev server proxies to the Spring Boot backend
  const response = await fetch(path, { ...options, headers })

  // A 401 with a token means the token expired or is invalid. Without a token it is just a failed login.
  if (response.status === 401 && token) {
    onUnauthorized()
  }
  if (!response.ok) {
    throw new ApiError(response.status)
  }
  // 204 No Content (DELETE) has no body to parse
  if (response.status === 204) {
    return undefined as T
  }
  return response.json()
}

export function login(body: LoginRequest) {
  return request<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify(body) })
}

export function register(body: RegisterRequest) {
  return request<AuthResponse>('/api/auth/register', { method: 'POST', body: JSON.stringify(body) })
}

export function searchGames(query: string) {
  return request<RawgSearchResponse>(`/api/games/search?q=${encodeURIComponent(query)}`)
}

export function getLibrary(status?: Status) {
  return request<LibraryEntryResponse[]>(status ? `/api/library?status=${status}` : '/api/library')
}

export function getLibraryStats() {
  return request<LibraryStatsResponse>('/api/library/stats')
}

export function addLibraryEntry(body: AddLibraryEntryRequest) {
  return request<LibraryEntryResponse>('/api/library', { method: 'POST', body: JSON.stringify(body) })
}

export function updateLibraryEntry(id: number, body: UpdateLibraryEntryRequest) {
  return request<LibraryEntryResponse>(`/api/library/${id}`, { method: 'PATCH', body: JSON.stringify(body) })
}

export function deleteLibraryEntry(id: number) {
  return request<void>(`/api/library/${id}`, { method: 'DELETE' })
}
