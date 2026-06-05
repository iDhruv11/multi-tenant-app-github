import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { User } from 'lucide-react'
import { useAuth } from '../providers/AuthProvider'
import { Button } from './ui/button'

export function Navbar() {
  const { tenant, user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <header className="relative flex h-14 items-center justify-between border-b border-border px-4">
      <div className="font-semibold">{tenant?.name ?? 'Workspace'}</div>
      <div className="relative">
        <Button variant="ghost" className="gap-2" onClick={() => setMenuOpen(!menuOpen)}>
          <User className="h-4 w-4" />
          {user ? `${user.firstName} ${user.lastName}` : 'Account'}
        </Button>
        {menuOpen && (
          <div className="absolute right-0 top-full z-50 mt-1 min-w-[140px] rounded-md border border-border bg-white py-1 shadow-md">
            <button
              type="button"
              className="block w-full px-3 py-2 text-left text-sm hover:bg-muted"
              onClick={() => {
                setMenuOpen(false)
                navigate('/sessions')
              }}
            >
              Sessions
            </button>
            <button
              type="button"
              className="block w-full px-3 py-2 text-left text-sm hover:bg-muted"
              onClick={() => {
                setMenuOpen(false)
                logout().then(() => navigate('/login'))
              }}
            >
              Logout
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
