import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listAuditLogs } from '../api/api'
import { formatDate } from '../lib/utils'
import { useAuth } from '../providers/AuthProvider'

export function AuditLogsPage() {
  const { isAdmin } = useAuth()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const logsQ = useQuery({ queryKey: ['audit-logs'], queryFn: listAuditLogs })

  if (!isAdmin) {
    return <div className="text-sm text-muted-foreground">Admin only.</div>
  }

  const selected = logsQ.data?.find((l) => l.id === selectedId)

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Audit Logs</h1>

      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-border text-muted-foreground">
            <th className="py-2">Time</th>
            <th>User</th>
            <th>Entity</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {logsQ.data?.map((log) => (
            <tr
              key={log.id}
              className="cursor-pointer border-b border-border hover:bg-muted"
              onClick={() => setSelectedId(log.id)}
            >
              <td className="py-2">{formatDate(log.createdAt)}</td>
              <td>{log.actorName}</td>
              <td>{log.entityType}</td>
              <td>{log.action}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {selected && (
        <div className="rounded border border-border p-4 text-sm">
          <div className="mb-2 font-medium">Selected Entry</div>
          <div>Actor: {selected.actorName}</div>
          <div>Entity: {selected.entityType}</div>
          <div>Action: {selected.action}</div>
          <div className="mt-2 font-medium">Changes</div>
          <pre className="mt-1 overflow-auto rounded bg-muted p-2 text-xs">{selected.changes || '{}'}</pre>
        </div>
      )}
    </div>
  )
}
