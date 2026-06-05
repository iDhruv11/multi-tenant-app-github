import { Navigate } from 'react-router-dom'
import { useAuth } from '../providers/AuthProvider'

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { accessToken, loading } = useAuth()
  if (loading) return <div className="p-8 text-sm text-muted-foreground">Loading...</div>
  if (!accessToken) return <Navigate to="/login" replace />
  return <>{children}</>
}
