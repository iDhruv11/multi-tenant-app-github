import { Link, useLocation } from 'react-router-dom'
import { cn } from '../lib/utils'
import { useAuth } from '../providers/AuthProvider'

const links = [
  { to: '/dashboard', label: 'Dashboard', roles: ['admin', 'manager', 'member'] },
  { to: '/projects', label: 'Projects', roles: ['admin', 'manager', 'member'] },
  { to: '/tasks', label: 'Tasks', roles: ['admin', 'manager', 'member'] },
  { to: '/users', label: 'Users', roles: ['admin', 'manager'] },
  { to: '/audit-logs', label: 'Audit Logs', roles: ['admin'] },
]

export function Sidebar() {
  const { pathname } = useLocation()
  const { user } = useAuth()
  const role = user?.role ?? 'member'

  return (
    <aside className="w-52 shrink-0 border-r border-border p-3">
      <nav className="flex flex-col gap-1">
        {links
          .filter((l) => l.roles.includes(role))
          .map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className={cn(
                'rounded px-3 py-2 text-sm',
                pathname.startsWith(link.to) ? 'bg-muted font-medium' : 'text-muted-foreground hover:bg-muted'
              )}
            >
              {link.label}
            </Link>
          ))}
      </nav>
    </aside>
  )
}
