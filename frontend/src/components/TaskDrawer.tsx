import type { ProjectMember, Task, TaskPriority, TaskStatus, User } from '../types/api'
import { formatDate, labelEnum } from '../lib/utils'
import { Button } from './ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader } from './ui/dialog'
import { Input } from './ui/input'
import { Label } from './ui/label'

type AssigneeOption = Pick<User, 'id' | 'firstName' | 'lastName'> | Pick<ProjectMember, 'userId' | 'firstName' | 'lastName'>

type ViewProps = {
  open: boolean
  task: Task | null
  users?: User[]
  onClose: () => void
}

export function TaskViewDialog({ open, task, users = [], onClose }: ViewProps) {
  if (!task) return null
  const assignee = users.find((u) => u.id === task.assignedToId)
  const assigneeName = task.assignedToName ?? (assignee ? `${assignee.firstName} ${assignee.lastName}` : '-')
  const creator = users.find((u) => u.id === task.createdById)

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent>
        <DialogHeader>Task Details</DialogHeader>
        <div className="space-y-2 text-sm">
          <div><Label>Title</Label><div>{task.title}</div></div>
          <div><Label>Description</Label><div>{task.description || '-'}</div></div>
          <div><Label>Status</Label><div>{labelEnum(task.status)}</div></div>
          <div><Label>Priority</Label><div>{labelEnum(task.priority)}</div></div>
          <div><Label>Assigned To</Label><div>{assigneeName}</div></div>
          <div><Label>Created By</Label><div>{creator ? `${creator.firstName} ${creator.lastName}` : '-'}</div></div>
          <div><Label>Created At</Label><div>{formatDate(task.createdAt)}</div></div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Close</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

type EditProps = {
  open: boolean
  task: Task | null
  assignees: AssigneeOption[]
  statusOnly?: boolean
  canDelete?: boolean
  onClose: () => void
  onSave: (data: { title: string; description: string; status: TaskStatus; priority: TaskPriority; assignedToId: string }) => void
  onDelete: () => void
}

function optionId(a: AssigneeOption) {
  return 'userId' in a ? a.userId : a.id
}

export function TaskEditDialog({ open, task, assignees, statusOnly = false, canDelete = true, onClose, onSave, onDelete }: EditProps) {
  if (!task) return null

  const assigneeName = task.assignedToName ?? 'Unassigned'

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const fd = new FormData(e.currentTarget)
    onSave({
      title: String(fd.get('title')),
      description: String(fd.get('description') ?? ''),
      status: String(fd.get('status')) as TaskStatus,
      priority: String(fd.get('priority')) as TaskPriority,
      assignedToId: String(fd.get('assignedToId') ?? task.assignedToId ?? ''),
    })
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent>
        <DialogHeader>Edit Task</DialogHeader>
        <form className="space-y-3" onSubmit={handleSubmit}>
          <div>
            <Label htmlFor="title">Title</Label>
            <Input id="title" name="title" defaultValue={task.title} required disabled={statusOnly} />
          </div>
          <div>
            <Label htmlFor="description">Description</Label>
            <Input id="description" name="description" defaultValue={task.description ?? ''} disabled={statusOnly} />
          </div>
          <div>
            <Label>Status</Label>
            <select name="status" defaultValue={task.status} className="h-9 w-full rounded-md border border-border px-2 text-sm">
              <option value="todo">TODO</option>
              <option value="in_progress">IN PROGRESS</option>
              <option value="done">DONE</option>
            </select>
          </div>
          <div>
            <Label>Priority</Label>
            <select name="priority" defaultValue={task.priority} disabled={statusOnly} className="h-9 w-full rounded-md border border-border px-2 text-sm disabled:opacity-60">
              <option value="low">LOW</option>
              <option value="medium">MEDIUM</option>
              <option value="high">HIGH</option>
            </select>
          </div>
          <div>
            <Label>Assigned To</Label>
            {statusOnly ? (
              <Input value={assigneeName} disabled />
            ) : (
              <select name="assignedToId" defaultValue={task.assignedToId ?? ''} className="h-9 w-full rounded-md border border-border px-2 text-sm">
                <option value="">Unassigned</option>
                {assignees.map((u) => (
                  <option key={optionId(u)} value={optionId(u)}>{u.firstName} {u.lastName}</option>
                ))}
              </select>
            )}
          </div>
          <DialogFooter>
            {canDelete && !statusOnly && (
              <Button type="button" variant="destructive" onClick={onDelete}>Delete</Button>
            )}
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit">Save</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
