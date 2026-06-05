import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listMembers } from '../api/api'
import { listProjects } from '../api/api'
import { assignTask, createTask, deleteTask, listAllTasks, updateTask } from '../api/api'
import { listUsers } from '../api/api'
import type { ProjectMemberRole, Task, TaskPriority, TaskStatus } from '../types/api'
import { TaskCard } from '../components/TaskCard'
import { TaskEditDialog, TaskViewDialog } from '../components/TaskDrawer'
import { Button } from '../components/ui/button'
import { useAuth } from '../providers/AuthProvider'

function taskPerms(role: ProjectMemberRole | undefined, isAdmin: boolean) {
  if (isAdmin) return { canEdit: true, canDelete: true, statusOnly: false }
  if (role === 'owner' || role === 'editor') return { canEdit: true, canDelete: true, statusOnly: false }
  if (role === 'viewer') return { canEdit: true, canDelete: false, statusOnly: true }
  return { canEdit: false, canDelete: false, statusOnly: false }
}

export function TasksPage() {
  const qc = useQueryClient()
  const { isAdmin } = useAuth()
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [assigneeFilter, setAssigneeFilter] = useState<string>('all')
  const [viewTask, setViewTask] = useState<Task | null>(null)
  const [editTask, setEditTask] = useState<Task | null>(null)

  const usersQ = useQuery({ queryKey: ['users'], queryFn: listUsers, enabled: isAdmin })
  const projectsQ = useQuery({ queryKey: ['projects'], queryFn: listProjects })
  const membersQ = useQuery({
    queryKey: ['project-members', editTask?.projectId],
    queryFn: () => listMembers(editTask!.projectId),
    enabled: !!editTask,
  })
  const tasksQ = useQuery({
    queryKey: ['all-tasks', statusFilter, assigneeFilter],
    queryFn: () =>
      listAllTasks({
        status: statusFilter === 'all' ? undefined : (statusFilter as TaskStatus),
        assigneeId: assigneeFilter === 'all' ? undefined : assigneeFilter,
      }),
  })

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['all-tasks'] })
    qc.invalidateQueries({ queryKey: ['projects'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
  }

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
        if (data.assignedToId) await assignTask(task.id, data.assignedToId)
      }
    },
    onSuccess: () => {
      setEditTask(null)
      invalidate()
    },
  })

  const deleteTaskM = useMutation({
    mutationFn: deleteTask,
    onSuccess: () => {
      setEditTask(null)
      invalidate()
    },
  })

  const users = usersQ.data ?? []
  const tasks = tasksQ.data ?? []
  const projectRoleMap = Object.fromEntries((projectsQ.data ?? []).map((p) => [p.id, p.myRole]))
  const editPerms = editTask ? taskPerms(projectRoleMap[editTask.projectId], isAdmin) : { canEdit: false, canDelete: false, statusOnly: false }

  const byStatus = {
    todo: tasks.filter((t) => t.status === 'todo'),
    in_progress: tasks.filter((t) => t.status === 'in_progress'),
    done: tasks.filter((t) => t.status === 'done'),
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Tasks</h1>
        {isAdmin && (
          <Button
            onClick={() => {
              const projectId = projectsQ.data?.[0]?.id
              if (!projectId) {
                alert('Create a project first')
                return
              }
              const title = prompt('Task title')
              if (title) {
                createTask(projectId, { title }).then(invalidate)
              }
            }}
          >
            Create Task
          </Button>
        )}
      </div>

      <div className="rounded border border-border p-3 text-sm">
        <div className="mb-2 font-medium">Filters</div>
        <div className="flex flex-wrap gap-4">
          <label>
            Status{' '}
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="ml-2 rounded border border-border px-2 py-1">
              <option value="all">All</option>
              <option value="todo">TODO</option>
              <option value="in_progress">IN PROGRESS</option>
              <option value="done">DONE</option>
            </select>
          </label>
          {isAdmin && (
            <label>
              Assignee{' '}
              <select value={assigneeFilter} onChange={(e) => setAssigneeFilter(e.target.value)} className="ml-2 rounded border border-border px-2 py-1">
                <option value="all">All</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>
                ))}
              </select>
            </label>
          )}
        </div>
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        {(['todo', 'in_progress', 'done'] as const).map((status) => (
          <div key={status} className="rounded border border-border p-2">
            <div className="mb-2 text-xs font-semibold">{status.replace('_', ' ').toUpperCase()}</div>
            {byStatus[status].map((task) => {
              const perms = taskPerms(projectRoleMap[task.projectId], isAdmin)
              return (
                <TaskCard
                  key={task.id}
                  task={task}
                  users={users}
                  canEdit={perms.canEdit}
                  canDelete={perms.canDelete}
                  onView={setViewTask}
                  onEdit={setEditTask}
                  onDelete={(t) => deleteTaskM.mutate(t.id)}
                />
              )
            })}
          </div>
        ))}
      </div>

      <TaskViewDialog open={!!viewTask} task={viewTask} users={users} onClose={() => setViewTask(null)} />
      <TaskEditDialog
        open={!!editTask}
        task={editTask}
        assignees={membersQ.data ?? []}
        statusOnly={editPerms.statusOnly}
        canDelete={editPerms.canDelete}
        onClose={() => setEditTask(null)}
        onSave={(data) => editTask && saveTaskM.mutate({ task: editTask, data, statusOnly: editPerms.statusOnly })}
        onDelete={() => editTask && deleteTaskM.mutate(editTask.id)}
      />
    </div>
  )
}
