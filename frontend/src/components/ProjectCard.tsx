import type { Project } from '../types/api'
import { labelEnum } from '../lib/utils'
import { Badge } from './ui/badge'
import { Card, CardContent, CardHeader } from './ui/card'

type Props = {
  project: Project
  selected: boolean
  onSelect: () => void
}

export function ProjectCard({ project, selected, onSelect }: Props) {
  return (
    <Card className={selected ? 'border-slate-800' : ''} onClick={onSelect}>
      <CardHeader className="cursor-pointer">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold">{project.name}</h3>
          <Badge>{labelEnum(String(project.visibility))}</Badge>
        </div>
        <p className="text-sm text-muted-foreground">{project.description || 'No description'}</p>
      </CardHeader>
      <CardContent className="text-xs text-muted-foreground">
        Members: {project.memberCount} · Open tasks: {project.openTaskCount} · Done: {project.doneTaskCount}
      </CardContent>
    </Card>
  )
}
