import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listSessions, revokeAll, revokeSession } from '../api/api'
import { formatDate } from '../lib/utils'
import { useAuth } from '../providers/AuthProvider'
import { Button } from '../components/ui/button'

export function SessionsPage() {
  const qc = useQueryClient()
  const { logout } = useAuth()
  const sessionsQ = useQuery({ queryKey: ['sessions'], queryFn: listSessions })

  const revokeM = useMutation({
    mutationFn: revokeSession,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sessions'] }),
  })

  const revokeAllM = useMutation({
    mutationFn: revokeAll,
    onSuccess: async () => {
      await logout()
      window.location.href = '/login'
    },
  })

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Sessions</h1>
        <Button variant="outline" onClick={() => revokeAllM.mutate()}>Logout All</Button>
      </div>

      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-border text-muted-foreground">
            <th className="py-2">Device</th>
            <th>Browser</th>
            <th>Last Active</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {sessionsQ.data?.map((s) => (
            <tr key={s.id} className="border-b border-border">
              <td className="py-2">{s.device || 'Unknown'}</td>
              <td>{s.browser || '-'}</td>
              <td>{formatDate(s.lastActive)}</td>
              <td>
                {s.current ? (
                  <span className="text-muted-foreground">Current</span>
                ) : (
                  <Button size="sm" variant="outline" onClick={() => revokeM.mutate(s.id)}>Revoke</Button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
