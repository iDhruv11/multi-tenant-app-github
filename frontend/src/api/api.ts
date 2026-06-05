import axios from 'axios'
import type {
  ApiWrap,
  AuditLog,
  AuthResponse,
  Dashboard,
  Project,
  ProjectMember,
  ProjectMemberRole,
  ProjectVisibility,
  Session,
  Task,
  TaskPriority,
  TaskStatus,
  Tenant,
  User,
  UserRemovalPreview,
  UserRole,
} from '../types/api'

const http = axios.create({ baseURL: '/api/v1' })

let getAccessToken: () => string | null = () => null
let getSessionId: () => string | null = () => null
let getRefreshToken: () => string | null = () => null
let onTokens: (access: string, refresh: string, sessionId: string) => void = () => {}
let onLogout: () => void = () => {}

export function setupAxiosAuth(handlers: {
  getAccessToken: () => string | null
  getSessionId: () => string | null
  getRefreshToken: () => string | null
  onTokens: (access: string, refresh: string, sessionId: string) => void
  onLogout: () => void
}) {
  getAccessToken = handlers.getAccessToken
  getSessionId = handlers.getSessionId
  getRefreshToken = handlers.getRefreshToken
  onTokens = handlers.onTokens
  onLogout = handlers.onLogout
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  const sid = getSessionId()
  if (sid) config.headers['X-Session-Id'] = sid
  return config
})

let refreshing: Promise<string | null> | null = null

async function doRefresh() {
  const rt = getRefreshToken()
  if (!rt) return null
  try {
    const res = await axios.post('/api/v1/auth/refresh', { refreshToken: rt })
    const data = res.data.data
    onTokens(data.accessToken, data.refreshToken, data.sessionId)
    return data.accessToken as string
  } catch {
    onLogout()
    if (typeof window !== 'undefined') window.location.href = '/login'
    return null
  }
}

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    if (!original || original._retry || (status !== 401 && status !== 403)) {
      if (status === 401) {
        onLogout()
        if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
          window.location.href = '/login'
        }
      }
      return Promise.reject(error)
    }
    original._retry = true
    if (!refreshing) refreshing = doRefresh().finally(() => { refreshing = null })
    const newToken = await refreshing
    if (!newToken) return Promise.reject(error)
    original.headers.Authorization = `Bearer ${newToken}`
    return http(original)
  }
)

export async function register(body: {
  tenantName: string
  tenantSlug: string
  email: string
  password: string
  firstName: string
  lastName: string
}) {
  const res = await http.post<ApiWrap<AuthResponse>>('/auth/register', body)
  return res.data.data
}

export async function login(body: { tenantSlug: string; email: string; password: string }) {
  const res = await http.post<ApiWrap<AuthResponse>>('/auth/login', body)
  return res.data.data
}

export async function logout(refreshToken: string) {
  await http.post('/auth/logout', { refreshToken })
}

export async function getMe() {
  const res = await http.get<ApiWrap<User>>('/users/me')
  return res.data.data
}

export async function listUsers() {
  const res = await http.get<ApiWrap<User[]>>('/users')
  return res.data.data
}

export async function createUser(body: {
  email: string
  password: string
  firstName: string
  lastName: string
  role: UserRole
}) {
  const res = await http.post<ApiWrap<User>>('/users', body)
  return res.data.data
}

export async function updateUser(
  id: string,
  body: { firstName?: string; lastName?: string; role?: UserRole; status?: 'active' | 'disabled' }
) {
  const res = await http.put<ApiWrap<User>>(`/users/${id}`, body)
  return res.data.data
}

export async function deleteUser(id: string) {
  await http.delete(`/users/${id}`)
}

export async function previewUserRemoval(id: string) {
  const res = await http.get<ApiWrap<UserRemovalPreview>>(`/users/${id}/removal-preview`)
  return res.data.data
}

export async function getTenant() {
  const res = await http.get<ApiWrap<Tenant>>('/tenant')
  return res.data.data
}

export async function listProjects() {
  const res = await http.get<ApiWrap<Project[]>>('/projects')
  return res.data.data
}

export async function createProject(body: { name: string; description?: string; visibility?: ProjectVisibility }) {
  const res = await http.post<ApiWrap<Project>>('/projects', body)
  return res.data.data
}

export async function updateProject(
  id: string,
  body: { name?: string; description?: string; visibility?: ProjectVisibility }
) {
  const res = await http.put<ApiWrap<Project>>(`/projects/${id}`, body)
  return res.data.data
}

export async function deleteProject(id: string) {
  await http.delete(`/projects/${id}`)
}

export async function listMembers(projectId: string) {
  const res = await http.get<ApiWrap<ProjectMember[]>>(`/projects/${projectId}/members`)
  return res.data.data
}

export async function addMember(projectId: string, userId: string, role: ProjectMemberRole) {
  await http.post(`/projects/${projectId}/members`, { userId, role })
}

export async function updateMemberRole(projectId: string, userId: string, role: ProjectMemberRole) {
  await http.put(`/projects/${projectId}/members/${userId}`, { role })
}

export async function removeMember(projectId: string, userId: string) {
  await http.delete(`/projects/${projectId}/members/${userId}`)
}

export async function listProjectActivity(projectId: string) {
  const res = await http.get<ApiWrap<{ content: AuditLog[] }>>(`/projects/${projectId}/activity`, { params: { size: 50 } })
  return res.data.data.content ?? (res.data.data as unknown as AuditLog[])
}

export async function listProjectTasks(projectId: string) {
  const res = await http.get<ApiWrap<Task[]>>(`/projects/${projectId}/tasks`)
  return res.data.data
}

export async function listAllTasks(params?: { status?: TaskStatus; assigneeId?: string }) {
  const res = await http.get<ApiWrap<Task[]>>('/tasks', { params })
  return res.data.data
}

export async function createTask(
  projectId: string,
  body: { title: string; description?: string; priority?: TaskPriority }
) {
  const res = await http.post<ApiWrap<Task>>(`/projects/${projectId}/tasks`, body)
  return res.data.data
}

export async function updateTask(
  id: string,
  body: { title?: string; description?: string; status?: TaskStatus; priority?: TaskPriority }
) {
  const res = await http.put<ApiWrap<Task>>(`/tasks/${id}`, body)
  return res.data.data
}

export async function deleteTask(id: string) {
  await http.delete(`/tasks/${id}`)
}

export async function assignTask(id: string, userId: string | null) {
  const res = await http.post<ApiWrap<Task>>(`/tasks/${id}/assign`, { userId })
  return res.data.data
}

export async function getDashboard() {
  const res = await http.get<ApiWrap<Dashboard>>('/dashboard')
  return res.data.data
}

export async function listAuditLogs() {
  const res = await http.get<ApiWrap<{ content: AuditLog[] }>>('/audit-logs', { params: { size: 100 } })
  const data = res.data.data
  if (Array.isArray(data)) return data
  return data.content ?? []
}

export async function listSessions() {
  const res = await http.get<ApiWrap<Session[]>>('/sessions')
  return res.data.data
}

export async function revokeSession(sessionId: string) {
  await http.delete(`/sessions/${sessionId}`)
}

export async function revokeAll() {
  await http.post('/sessions/revoke-all')
}
