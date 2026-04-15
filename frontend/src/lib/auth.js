const AUTH_STORAGE_KEY = 'mes-auth-session'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

function resolveApiBaseUrl() {
  if (API_BASE_URL) {
    return API_BASE_URL.replace(/\/$/, '')
  }

  const protocol = window.location.protocol
  const hostname = window.location.hostname
  const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1'
  const currentPort = window.location.port
  const port = isLocalhost && currentPort !== '8080' ? '8080' : currentPort
  return `${protocol}//${hostname}${port ? `:${port}` : ''}`
}

async function postJson(path, payload) {
  let response
  try {
    response = await fetch(`${resolveApiBaseUrl()}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })
  } catch {
    throw new Error('Unable to reach the backend server.')
  }

  let result = null
  try {
    result = await response.json()
  } catch {
    throw new Error('Failed to read server response.')
  }

  if (!response.ok || !result.success) {
    throw new Error(result.message || 'Request failed.')
  }

  return result.data
}

export async function login(credentials) {
  const session = await postJson('/api/auth/login', credentials)
  persistSession(session)
  return session
}

export async function signUp(payload) {
  const session = await postJson('/api/auth/signup', payload)
  persistSession(session)
  return session
}

export function persistSession(session) {
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
}

export function getStoredSession() {
  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw)
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export function isAuthenticated() {
  return Boolean(getStoredSession()?.token)
}

export function getCurrentUser() {
  return getStoredSession()?.user ?? null
}

export function logout() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY)
}
