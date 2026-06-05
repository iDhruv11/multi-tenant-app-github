import type { Task, User } from '../types/api'
import { labelEnum } from '../lib/utils'
import { Badge } from './ui/badge'
import { Button } from './ui/button'

type Props = {
  task: Task
  users?: User[]
  canEdit?: boolean
  canDelete?: boolean
  onView: (task: Task) => void
  onEdit: (task: Task) => void
  onDelete: (task: Task) => void
}

export function TaskCard({ task, users = [], canEdit = true, canDelete = true, onView, onEdit, onDelete }: Props) {
  const assignee = users.find((u) => u.id === task.assignedToId)
  const name = task.assignedToName ?? (assignee ? `${assignee.firstName} ${assignee.lastName}` : 'Unassigned')

  return (
    <div className="mb-2 rounded border border-border bg-white p-3 text-sm">
      <div className="font-medium">{task.title}</div>
      <div className="text-xs text-muted-foreground">{task.projectName}</div>
      <div className="mt-2 flex items-center gap-2">
        <Badge>{labelEnum(task.priority)}</Badge>
        <span className="text-xs text-muted-foreground">{name}</span>
      </div>
      <div className="mt-2 flex gap-2">
        <Button size="sm" variant="outline" onClick={() => onView(task)}>
          View
        </Button>
        {canEdit && (
          <Button size="sm" variant="outline" onClick={() => onEdit(task)}>
            Edit
          </Button>
        )}
        {canDelete && (
          <Button size="sm" variant="outline" onClick={() => onDelete(task)}>
            Del
          </Button>
        )}
      </div>
    </div>
  )
}
