import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { getMe, getTenant, logout as apiLogout, setupAxiosAuth } from '../api/api'
import type { Tenant, User } from '../types/api'

type AuthContextValue = {
  accessToken: string | null
  user: User | null
  tenant: Tenant | null
  loading: boolean
  login: (access: string, refresh: string, sessionId: string) => Promise<void>
  logout: () => Promise<void>
  isAdmin: boolean
  isManager: boolean
}

const STORAGE_KEY = 'saas_auth'

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

function loadStored() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as { accessToken: string; refreshToken: string; sessionId: string }
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [user, setUser] = useState<User | null>(null)
  const [tenant, setTenant] = useState<Tenant | null>(null)
  const [loading, setLoading] = useState(true)

  const tokensRef = useRef({ access: null as string | null, refresh: null as string | null, session: null as string | null })

  const saveTokens = useCallback((access: string, refresh: string, sessionId: string) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ accessToken: access, refreshToken: refresh, sessionId }))
    tokensRef.current = { access, refresh, session: sessionId }
    setAccessToken(access)
  }, [])

  const clearAuth = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY)
    tokensRef.current = { access: null, refresh: null, session: null }
    setAccessToken(null)
    setUser(null)
    setTenant(null)
    setLoading(false)
  }, [])

  useEffect(() => {
    setupAxiosAuth({
      getAccessToken: () => tokensRef.current.access,
      getSessionId: () => tokensRef.current.session,
      getRefreshToken: () => tokensRef.current.refresh,
      onTokens: saveTokens,
      onLogout: clearAuth,
    })
  }, [saveTokens, clearAuth])

  const loadUserData = useCallback(async () => {
    const me = await getMe()
    const t = await getTenant()
    setUser(me)
    setTenant(t)
    setLoading(false)
  }, [])

  useEffect(() => {
    const stored = loadStored()
    if (!stored) {
      setLoading(false)
      return
    }
    tokensRef.current = { access: stored.accessToken, refresh: stored.refreshToken, session: stored.sessionId }
    setAccessToken(stored.accessToken)
    loadUserData().catch(() => clearAuth())
  }, [loadUserData, clearAuth])

  const login = useCallback(
    async (access: string, refresh: string, sessionId: string) => {
      saveTokens(access, refresh, sessionId)
      await loadUserData()
    },
    [saveTokens, loadUserData]
  )

  const logout = useCallback(async () => {
    const rt = tokensRef.current.refresh
    if (rt) {
      try {
        await apiLogout(rt)
      } catch {
        // ignore
      }
    }
    clearAuth()
  }, [clearAuth])

  const value = useMemo(
    () => ({
      accessToken,
      user,
      tenant,
      loading,
      login,
      logout,
      isAdmin: user?.role === 'admin',
      isManager: user?.role === 'manager' || user?.role === 'admin',
    }),
    [accessToken, user, tenant, loading, login, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
