export type UserRole = 'admin' | 'manager' | 'member'
export type UserStatus = 'active' | 'disabled'
export type TaskStatus = 'todo' | 'in_progress' | 'done'
export type TaskPriority = 'low' | 'medium' | 'high'
export type ProjectVisibility = 'PRIVATE' | 'INTERNAL' | 'private' | 'internal'
export type ProjectMemberRole = 'owner' | 'editor' | 'viewer' | null

export type ApiWrap<T> = {
  data: T
  timestamp: string
  requestId: string
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  sessionId: string
  accessTokenExpiresInSeconds: number
}

export type User = {
  id: string
  email: string
  firstName: string
  lastName: string
  role: UserRole
  status: UserStatus
  createdAt: string
}

export type UserRemovalPreview = {
  projectsToDelete: { id: string; name: string }[]
  blocked: boolean
  message: string | null
}

export type Project = {
  id: string
  name: string
  description: string | null
  visibility: ProjectVisibility
  ownerId: string
  createdAt: string
  memberCount: number
  openTaskCount: number
  doneTaskCount: number
  myRole: ProjectMemberRole
}

export type Task = {
  id: string
  projectId: string
  projectName: string
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  assignedToId: string | null
  assignedToName: string | null
  createdById: string
  createdAt: string
}

export type ProjectMember = {
  userId: string
  firstName: string
  lastName: string
  email: string
  tenantRole: UserRole
  projectRole: ProjectMemberRole
  status: UserStatus
}

export type AuditLog = {
  id: string
  entityType: string
  entityId: string
  action: string
  actorId: string
  actorName: string
  changes: string | null
  createdAt: string
}

export type Session = {
  id: string
  device: string
  browser: string
  lastActive: string
  current: boolean
}

export type Dashboard = {
  projectCount: number
  userCount: number
  openTaskCount: number
  completedTaskCount: number
  recentActivity: AuditLog[]
  myTasks: Task[]
}

export type Tenant = {
  id: string
  name: string
  slug: string
}
