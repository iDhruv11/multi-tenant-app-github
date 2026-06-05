import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  addMember,
  createProject,
  deleteProject,
  listMembers,
  listProjectActivity,
  listProjects,
  removeMember,
  updateMemberRole,
  updateProject,
} from '../api/api'
import { assignTask, createTask, deleteTask, listProjectTasks, updateTask } from '../api/api'
import { listUsers } from '../api/api'
import type { Project, ProjectMemberRole, ProjectVisibility, Task, TaskPriority, TaskStatus } from '../types/api'
import { formatDate, labelEnum } from '../lib/utils'
import { useAuth } from '../providers/AuthProvider'
import { ProjectCard } from '../components/ProjectCard'
import { TaskCard } from '../components/TaskCard'
import { TaskEditDialog, TaskViewDialog } from '../components/TaskDrawer'
import { Button } from '../components/ui/button'
import { Card, CardContent, CardHeader } from '../components/ui/card'
import { Dialog, DialogContent, DialogFooter, DialogHeader } from '../components/ui/dialog'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs'

export function ProjectsPage() {
  const qc = useQueryClient()
  const { isAdmin, isManager } = useAuth()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [tab, setTab] = useState('overview')
  const [showCreate, setShowCreate] = useState(false)
  const [showEditProject, setShowEditProject] = useState(false)
  const [viewTask, setViewTask] = useState<Task | null>(null)
  const [editTask, setEditTask] = useState<Task | null>(null)
  const [editingMember, setEditingMember] = useState<string | null>(null)
  const [addingMember, setAddingMember] = useState(false)
  const [selectedAudit, setSelectedAudit] = useState<string | null>(null)

  const projectsQ = useQuery({ queryKey: ['projects'], queryFn: listProjects })
  const canListUsers =
    isAdmin ||
    isManager ||
    (projectsQ.data?.some((p) => p.myRole === 'owner' || p.myRole === 'editor') ?? false)
  const usersQ = useQuery({
    queryKey: ['users'],
    queryFn: listUsers,
    enabled: canListUsers,
  })
  const tasksQ = useQuery({
    queryKey: ['project-tasks', selectedId],
    queryFn: () => listProjectTasks(selectedId!),
    enabled: !!selectedId,
  })
  const membersQ = useQuery({
    queryKey: ['project-members', selectedId],
    queryFn: () => listMembers(selectedId!),
    enabled: !!selectedId,
  })

  const selected = projectsQ.data?.find((p) => p.id === selectedId) ?? null
  const users = usersQ.data ?? []
  const isOwner = isAdmin || selected?.myRole === 'owner'
  const isEditorOnly = !isAdmin && selected?.myRole === 'editor'
  const canEditProject = isOwner
  const canAddMembers = isOwner || isEditorOnly
  const canManageTasks = isOwner || isEditorOnly
  const canSeeActivity = isOwner
  const ownerCount = membersQ.data?.filter((m) => m.projectRole === 'owner').length ?? 0
  const isSoleOwner = (userId: string) => {
    const m = membersQ.data?.find((x) => x.userId === userId)
    return m?.projectRole === 'owner' && ownerCount <= 1
  }
  const memberIds = new Set(membersQ.data?.map((m) => m.userId) ?? [])
  const availableUsers = users.filter((u) => u.status === 'active' && !memberIds.has(u.id))
  const activityQ = useQuery({
    queryKey: ['project-activity', selectedId],
    queryFn: () => listProjectActivity(selectedId!),
    enabled: !!selectedId && tab === 'activity',
  })

  const invalidateProject = () => {
    qc.invalidateQueries({ queryKey: ['projects'] })
    if (selectedId) {
      qc.invalidateQueries({ queryKey: ['project-tasks', selectedId] })
      qc.invalidateQueries({ queryKey: ['project-members', selectedId] })
      qc.invalidateQueries({ queryKey: ['project-activity', selectedId] })
    }
  }

  const createProjectM = useMutation({
    mutationFn: createProject,
    onSuccess: (p) => {
      invalidateProject()
      setSelectedId(p.id)
      setShowCreate(false)
    },
  })

  const updateProjectM = useMutation({
    mutationFn: ({ id, body }: { id: string; body: Parameters<typeof updateProject>[1] }) => updateProject(id, body),
    onSuccess: () => {
      invalidateProject()
      setShowEditProject(false)
    },
  })

  const deleteProjectM = useMutation({
    mutationFn: deleteProject,
    onSuccess: () => {
      setSelectedId(null)
      invalidateProject()
    },
  })

  const createTaskM = useMutation({
    mutationFn: ({ projectId, title }: { projectId: string; title: string }) =>
      createTask(projectId, { title, priority: 'medium' }),
    onSuccess: invalidateProject,
  })

  const saveTaskM = useMutation({
    mutationFn: async ({ task, data, statusOnly }: { task: Task; data: { title: string; description: string; status: TaskStatus; priority: TaskPriority; assignedToId: string }; statusOnly: boolean }) => {
      if (statusOnly) {
        await updateTask(task.id, { status: data.status })
      } else {
        await updateTask(task.id, {
          title: data.title,
          description: data.description,
          status: data.status,
          priority: data.priority,
        })
        if (data.assignedToId) {
          await assignTask(task.id, data.assignedToId)
        }
      }
    },
    onSuccess: () => {
      setEditTask(null)
      invalidateProject()
    },
  })

  const deleteTaskM = useMutation({
    mutationFn: deleteTask,
    onSuccess: () => {
      setEditTask(null)
      invalidateProject()
    },
  })

  const owner = membersQ.data?.find((m) => m.userId === selected?.ownerId) ?? users.find((u) => u.id === selected?.ownerId)

  function taskPerms() {
    if (isAdmin || selected?.myRole === 'owner' || selected?.myRole === 'editor') {
      return { canEdit: true, canDelete: true, statusOnly: false }
    }
    if (selected?.myRole === 'viewer') {
      return { canEdit: true, canDelete: false, statusOnly: true }
    }
    return { canEdit: false, canDelete: false, statusOnly: false }
  }

  const taskPermsForProject = taskPerms()
  const editTaskPerms = editTask ? taskPerms() : { canEdit: false, canDelete: false, statusOnly: false }

  const tasksByStatus = {
    todo: tasksQ.data?.filter((t) => t.status === 'todo') ?? [],
    in_progress: tasksQ.data?.filter((t) => t.status === 'in_progress') ?? [],
    done: tasksQ.data?.filter((t) => t.status === 'done') ?? [],
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Projects</h1>
        {isAdmin && <Button onClick={() => setShowCreate(true)}>Create Project</Button>}
      </div>

      <div className="grid gap-4 lg:grid-cols-[280px_1fr]">
        <div className="space-y-2">
          {projectsQ.data?.map((p) => (
            <ProjectCard key={p.id} project={p} selected={p.id === selectedId} onSelect={() => setSelectedId(p.id)} />
          ))}
          {projectsQ.data?.length === 0 && <p className="text-sm text-muted-foreground">No projects yet</p>}
        </div>

        <div>
          {!selected && <p className="text-sm text-muted-foreground">Select a project</p>}
          {selected && (
            <Card>
              <CardHeader>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h2 className="text-lg font-semibold">{selected.name}</h2>
                    <p className="text-sm text-muted-foreground">{selected.description}</p>
                  </div>
                  {canEditProject && <Button variant="outline" size="sm" onClick={() => setShowEditProject(true)}>Edit Project</Button>}
                </div>
              </CardHeader>
              <CardContent>
                <Tabs value={tab} onValueChange={setTab}>
                  <TabsList>
                    <TabsTrigger value="overview">Overview</TabsTrigger>
                    <TabsTrigger value="tasks">Tasks</TabsTrigger>
                    <TabsTrigger value="members">Members</TabsTrigger>
                    {canSeeActivity && <TabsTrigger value="activity">Activity</TabsTrigger>}
                  </TabsList>

                  <TabsContent value="overview">
                    <div className="space-y-4 text-sm">
                      <div>
                        <div className="font-medium">Project Information</div>
                        <div>Name: {selected.name}</div>
                        <div>Owner: {owner ? `${owner.firstName} ${owner.lastName}` : '-'}</div>
                        <div>Visibility: {labelEnum(String(selected.visibility))}</div>
                        <div>Created At: {formatDate(selected.createdAt)}</div>
                      </div>
                      <div>
                        <div className="font-medium">Statistics</div>
                        <div>Members: {selected.memberCount}</div>
                        <div>Open Tasks: {selected.openTaskCount}</div>
                        <div>Done Tasks: {selected.doneTaskCount}</div>
                      </div>
                    </div>
                  </TabsContent>

                  <TabsContent value="tasks">
                    {canManageTasks && (
                      <Button
                        size="sm"
                        className="mb-3"
                        onClick={() => {
                          const title = prompt('Task title')
                          if (title) createTaskM.mutate({ projectId: selected.id, title })
                        }}
                      >
                        Add Task
                      </Button>
                    )}
                    <div className="grid gap-3 md:grid-cols-3">
                      {(['todo', 'in_progress', 'done'] as const).map((status) => (
                        <div key={status} className="rounded border border-border p-2">
                          <div className="mb-2 text-xs font-semibold">{labelEnum(status)}</div>
                          {tasksByStatus[status].map((task) => (
                            <TaskCard
                              key={task.id}
                              task={task}
                              users={users}
                              canEdit={taskPermsForProject.canEdit}
                              canDelete={taskPermsForProject.canDelete}
                              onView={setViewTask}
                              onEdit={setEditTask}
                              onDelete={(t) => deleteTaskM.mutate(t.id)}
                            />
                          ))}
                        </div>
                      ))}
                    </div>
                  </TabsContent>

                  <TabsContent value="members">
                    {canAddMembers && (
                      <Button size="sm" className="mb-3" onClick={() => setAddingMember(true)}>Add Member</Button>
                    )}
                    <table className="w-full text-left text-sm">
                      <thead>
                        <tr className="border-b border-border text-muted-foreground">
                          <th className="py-2">Name</th>
                          <th>Tenant Role</th>
                          <th>Project Role</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {membersQ.data?.map((m) => (
                          <tr key={m.userId} className="border-b border-border">
                            {editingMember === m.userId ? (
                              <td colSpan={4} className="py-2">
                                <form
                                  className="flex flex-wrap items-center gap-2"
                                  onSubmit={(e) => {
                                    e.preventDefault()
                                    const fd = new FormData(e.currentTarget)
                                    updateMemberRole(selected.id, m.userId, fd.get('role') as ProjectMemberRole).then(() => {
                                      setEditingMember(null)
                                      invalidateProject()
                                    })
                                  }}
                                >
                                  <span>{m.firstName} {m.lastName}</span>
                                  <select name="role" defaultValue={m.projectRole} className="rounded border border-border px-2 py-1 text-sm">
                                    <option value="owner">OWNER</option>
                                    <option value="editor">EDITOR</option>
                                    <option value="viewer">VIEWER</option>
                                  </select>
                                  <Button type="submit" size="sm">Save</Button>
                                  <Button type="button" size="sm" variant="outline" onClick={() => setEditingMember(null)}>Cancel</Button>
                                </form>
                              </td>
                            ) : (
                              <>
                                <td className="py-2">{m.firstName} {m.lastName}</td>
                                <td>{labelEnum(m.tenantRole)}</td>
                                <td>{labelEnum(m.projectRole)}</td>
                                <td className="space-x-2">
                                  {isOwner && (
                                    <Button
                                      size="sm"
                                      variant="outline"
                                      disabled={isSoleOwner(m.userId)}
                                      onClick={() => setEditingMember(m.userId)}
                                    >
                                      Edit
                                    </Button>
                                  )}
                                  {(isOwner || (isEditorOnly && m.projectRole === 'viewer')) && (
                                    <Button
                                      size="sm"
                                      variant="outline"
                                      disabled={isSoleOwner(m.userId)}
                                      onClick={() => {
                                        removeMember(selected.id, m.userId)
                                          .then(invalidateProject)
                                          .catch((err) => {
                                            const msg = err.response?.data?.detail
                                            if (msg) alert(msg)
                                          })
                                      }}
                                    >
                                      Delete
                                    </Button>
                                  )}
                                </td>
                              </>
                            )}
                          </tr>
                        ))}
                        {addingMember && (
                          <tr>
                            <td colSpan={4} className="py-2">
                              <form
                                className="flex flex-wrap items-center gap-2"
                                onSubmit={(e) => {
                                  e.preventDefault()
                                  const fd = new FormData(e.currentTarget)
                                  addMember(selected.id, String(fd.get('userId')), fd.get('role') as ProjectMemberRole).then(() => {
                                    setAddingMember(false)
                                    invalidateProject()
                                  })
                                }}
                              >
                                <select name="userId" className="rounded border border-border px-2 py-1 text-sm" required>
                                  <option value="">Pick user</option>
                                  {availableUsers.map((u) => (
                                    <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>
                                  ))}
                                </select>
                                {isOwner ? (
                                  <select name="role" defaultValue="viewer" className="rounded border border-border px-2 py-1 text-sm">
                                    <option value="editor">EDITOR</option>
                                    <option value="viewer">VIEWER</option>
                                    <option value="owner">OWNER</option>
                                  </select>
                                ) : (
                                  <select name="role" defaultValue="viewer" disabled className="rounded border border-border px-2 py-1 text-sm opacity-60">
                                    <option value="viewer">VIEWER</option>
                                  </select>
                                )}
                                <Button type="submit" size="sm">Save</Button>
                                <Button type="button" size="sm" variant="outline" onClick={() => setAddingMember(false)}>Cancel</Button>
                              </form>
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </TabsContent>

                  <TabsContent value="activity">
                    <table className="w-full text-left text-sm">
                      <thead>
                        <tr className="border-b border-border text-muted-foreground">
                          <th className="py-2">Timestamp</th>
                          <th>User</th>
                          <th>Entity</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {activityQ.data?.map((a) => (
                          <tr
                            key={a.id}
                            className="cursor-pointer border-b border-border hover:bg-muted"
                            onClick={() => setSelectedAudit(a.id)}
                          >
                            <td className="py-2">{formatDate(a.createdAt)}</td>
                            <td>{a.actorName}</td>
                            <td>{a.entityType}</td>
                            <td>{a.action}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    {selectedAudit && (
                      <div className="mt-4 rounded border border-border p-3 text-sm">
                        <div className="font-medium">Selected Entry</div>
                        {(() => {
                          const a = activityQ.data?.find((x) => x.id === selectedAudit)
                          if (!a) return null
                          return (
                            <>
                              <div>Actor: {a.actorName}</div>
                              <div>Entity: {a.entityType}</div>
                              <div>Action: {a.action}</div>
                              <pre className="mt-2 overflow-auto rounded bg-muted p-2 text-xs">{a.changes || '{}'}</pre>
                            </>
                          )
                        })()}
                      </div>
                    )}
                  </TabsContent>
                </Tabs>
              </CardContent>
            </Card>
          )}
        </div>
      </div>

      <Dialog open={showCreate} onOpenChange={setShowCreate}>
        <DialogContent>
          <DialogHeader>Create Project</DialogHeader>
          <form
            className="space-y-3"
            onSubmit={(e) => {
              e.preventDefault()
              const fd = new FormData(e.currentTarget)
              createProjectM.mutate({
                name: String(fd.get('name')),
                description: String(fd.get('description') ?? ''),
                visibility: (fd.get('visibility') as ProjectVisibility) || 'PRIVATE',
              })
            }}
          >
            <div><Label>Name</Label><Input name="name" required /></div>
            <div><Label>Description</Label><Input name="description" /></div>
            <div>
              <Label>Visibility</Label>
              <select name="visibility" defaultValue="PRIVATE" className="h-9 w-full rounded-md border border-border px-2 text-sm">
                <option value="PRIVATE">PRIVATE</option>
                <option value="INTERNAL">INTERNAL</option>
              </select>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setShowCreate(false)}>Cancel</Button>
              <Button type="submit">Create</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {selected && (
        <Dialog open={showEditProject} onOpenChange={setShowEditProject}>
          <DialogContent>
            <DialogHeader>Edit Project</DialogHeader>
            <form
              className="space-y-3"
              onSubmit={(e) => {
                e.preventDefault()
                const fd = new FormData(e.currentTarget)
                updateProjectM.mutate({
                  id: selected.id,
                  body: {
                    name: String(fd.get('name')),
                    description: String(fd.get('description') ?? ''),
                    visibility: fd.get('visibility') as ProjectVisibility,
                  },
                })
              }}
            >
              <div><Label>Name</Label><Input name="name" defaultValue={selected.name} required /></div>
              <div><Label>Description</Label><Input name="description" defaultValue={selected.description ?? ''} /></div>
              <div>
                <Label>Visibility</Label>
                <select name="visibility" defaultValue={String(selected.visibility)} className="h-9 w-full rounded-md border border-border px-2 text-sm">
                  <option value="PRIVATE">PRIVATE</option>
                  <option value="INTERNAL">INTERNAL</option>
                </select>
              </div>
              <DialogFooter>
                <Button type="button" variant="destructive" onClick={() => deleteProjectM.mutate(selected.id)}>Delete</Button>
                <Button type="button" variant="outline" onClick={() => setShowEditProject(false)}>Cancel</Button>
                <Button type="submit">Save</Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      )}

      <TaskViewDialog open={!!viewTask} task={viewTask} users={users} onClose={() => setViewTask(null)} />
      <TaskEditDialog
        open={!!editTask}
        task={editTask}
        assignees={membersQ.data ?? []}
        statusOnly={editTaskPerms.statusOnly}
        canDelete={editTaskPerms.canDelete}
        onClose={() => setEditTask(null)}
        onSave={(data) => editTask && saveTaskM.mutate({ task: editTask, data, statusOnly: editTaskPerms.statusOnly })}
        onDelete={() => editTask && deleteTaskM.mutate(editTask.id)}
      />
    </div>
  )
}
