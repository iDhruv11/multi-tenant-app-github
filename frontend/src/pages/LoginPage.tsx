import { FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../api/api'
import { useAuth } from '../providers/AuthProvider'
import { Button } from '../components/ui/button'
import { Card, CardContent, CardHeader } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'

export function LoginPage() {
  const { login: saveAuth } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    const fd = new FormData(e.target as HTMLFormElement)
    try {
      const res = await login({
        tenantSlug: String(fd.get('tenantSlug')),
        email: String(fd.get('email')),
        password: String(fd.get('password')),
      })
      await saveAuth(res.accessToken, res.refreshToken, res.sessionId)
      navigate('/dashboard')
    } catch {
      setError('Login failed. Check workspace slug, email and password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader>Login</CardHeader>
        <CardContent>
          <form className="space-y-3" onSubmit={onSubmit}>
            <div>
              <Label htmlFor="tenantSlug">Workspace Slug</Label>
              <Input id="tenantSlug" name="tenantSlug" required />
            </div>
            <div>
              <Label htmlFor="email">Email</Label>
              <Input id="email" name="email" type="email" required />
            </div>
            <div>
              <Label htmlFor="password">Password</Label>
              <Input id="password" name="password" type="password" required />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button type="submit" className="w-full" disabled={loading}>{loading ? 'Logging in...' : 'Login'}</Button>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            No workspace? <Link className="underline" to="/register">Sign up</Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
