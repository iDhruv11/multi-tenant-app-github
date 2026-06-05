import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createUser, deleteUser, listUsers, previewUserRemoval, updateUser } from '../api/api'
import type { User, UserRole } from '../types/api'
import { formatDate, labelEnum } from '../lib/utils'
import { Button } from '../components/ui/button'
import { useAuth } from '../providers/AuthProvider'

function apiError(err: unknown) {
  const msg = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail
  if (msg) alert(msg)
}

async function confirmRemoval(user: User, action: 'delete' | 'disable') {
  const preview = await previewUserRemoval(user.id)
  if (preview.blocked) {
    alert(preview.message ?? 'Cannot remove this user')
    return false
  }
  let msg = action === 'delete'
    ? `Delete ${user.firstName} ${user.lastName}?`
    : `Disable ${user.firstName} ${user.lastName}?`
  if (preview.projectsToDelete.length > 0) {
    const names = preview.projectsToDelete.map((p) => p.name).join(', ')
    msg += `\n\nThese projects will be deleted (and all their tasks):\n${names}`
  }
  return confirm(msg)
}

export function UsersPage() {
  const qc = useQueryClient()
  const { isAdmin, isManager } = useAuth()
  const [editingId, setEditingId] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)

  const usersQ = useQuery({ queryKey: ['users'], queryFn: listUsers })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['users'] })

  const activeAdmins = usersQ.data?.filter((u) => u.role === 'admin' && u.status === 'active') ?? []
  const isOnlyActiveAdmin = (u: User) => u.role === 'admin' && u.status === 'active' && activeAdmins.length <= 1

  const createM = useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      setAdding(false)
      invalidate()
    },
    onError: apiError,
  })

  const updateM = useMutation({
    mutationFn: ({ id, body }: { id: string; body: Parameters<typeof updateUser>[1] }) => updateUser(id, body),
    onSuccess: () => {
      setEditingId(null)
      invalidate()
    },
    onError: apiError,
  })

  const deleteM = useMutation({
    mutationFn: deleteUser,
    onSuccess: invalidate,
    onError: apiError,
  })

  if (!isManager) {
    return <div className="text-sm text-muted-foreground">You do not have access to this page.</div>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Users</h1>
        {isAdmin && <Button onClick={() => setAdding(true)}>Create User</Button>}
      </div>

      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-border text-muted-foreground">
            <th className="py-2">Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Status</th>
            <th>Created</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {usersQ.data?.map((u: User) =>
            editingId === u.id ? (
              <tr key={u.id}>
                <td colSpan={6} className="py-2">
                  <form
                    className="flex flex-wrap items-center gap-2"
                    onSubmit={async (e) => {
                      e.preventDefault()
                      const fd = new FormData(e.currentTarget)
                      const body = {
                        firstName: String(fd.get('firstName')),
                        lastName: String(fd.get('lastName')),
                        role: fd.get('role') as UserRole,
                        status: fd.get('status') as 'active' | 'disabled',
                      }
                      const disabling = body.status === 'disabled' && u.status === 'active'
                      if (disabling) {
                        const ok = await confirmRemoval(u, 'disable')
                        if (!ok) return
                      }
                      updateM.mutate({ id: u.id, body })
                    }}
                  >
                    <input name="firstName" defaultValue={u.firstName} className="rounded border border-border px-2 py-1" />
                    <input name="lastName" defaultValue={u.lastName} className="rounded border border-border px-2 py-1" />
                    <select name="role" defaultValue={u.role} disabled={isOnlyActiveAdmin(u)} className="rounded border border-border px-2 py-1 disabled:opacity-60">
                      <option value="admin">ADMIN</option>
                      <option value="manager">MANAGER</option>
                      <option value="member">MEMBER</option>
                    </select>
                    <select name="status" defaultValue={u.status} disabled={isOnlyActiveAdmin(u)} className="rounded border border-border px-2 py-1 disabled:opacity-60">
                      <option value="active">ACTIVE</option>
                      <option value="disabled">DISABLED</option>
                    </select>
                    <Button type="submit" size="sm">Save</Button>
                    <Button type="button" size="sm" variant="outline" onClick={() => setEditingId(null)}>Cancel</Button>
                  </form>
                </td>
              </tr>
            ) : (
              <tr key={u.id} className="border-b border-border">
                <td className="py-2">{u.firstName} {u.lastName}</td>
                <td>{u.email}</td>
                <td>{labelEnum(u.role)}</td>
                <td>{labelEnum(u.status)}</td>
                <td>{formatDate(u.createdAt)}</td>
                <td className="space-x-2">
                  {isAdmin && (
                    <>
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={isOnlyActiveAdmin(u)}
                        onClick={() => setEditingId(u.id)}
                      >
                        Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={isOnlyActiveAdmin(u)}
                        onClick={async () => {
                          const ok = await confirmRemoval(u, 'delete')
                          if (ok) deleteM.mutate(u.id)
                        }}
                      >
                        Delete
                      </Button>
                    </>
                  )}
                </td>
              </tr>
            )
          )}
          {adding && (
            <tr>
              <td colSpan={6} className="py-2">
                <form
                  className="flex flex-wrap items-center gap-2"
                  onSubmit={(e) => {
                    e.preventDefault()
                    const fd = new FormData(e.currentTarget)
                    createM.mutate({
                      email: String(fd.get('email')),
                      password: String(fd.get('password')),
                      firstName: String(fd.get('firstName')),
                      lastName: String(fd.get('lastName')),
                      role: fd.get('role') as UserRole,
                    })
                  }}
                >
                  <input name="firstName" placeholder="First" required className="rounded border border-border px-2 py-1" />
                  <input name="lastName" placeholder="Last" required className="rounded border border-border px-2 py-1" />
                  <input name="email" type="email" placeholder="Email" required className="rounded border border-border px-2 py-1" />
                  <input name="password" type="password" placeholder="Password" required className="rounded border border-border px-2 py-1" />
                  <select name="role" defaultValue="member" className="rounded border border-border px-2 py-1">
                    <option value="member">MEMBER</option>
                    <option value="manager">MANAGER</option>
                    <option value="admin">ADMIN</option>
                  </select>
                  <Button type="submit" size="sm">Save</Button>
                  <Button type="button" size="sm" variant="outline" onClick={() => setAdding(false)}>Cancel</Button>
                </form>
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
