import { FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api/api'
import { useAuth } from '../providers/AuthProvider'
import { Button } from '../components/ui/button'
import { Card, CardContent, CardHeader } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'

export function RegisterPage() {
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
      const res = await register({
        tenantName: String(fd.get('tenantName')),
        tenantSlug: String(fd.get('tenantSlug')),
        email: String(fd.get('email')),
        password: String(fd.get('password')),
        firstName: String(fd.get('firstName')),
        lastName: String(fd.get('lastName')),
      })
      await saveAuth(res.accessToken, res.refreshToken, res.sessionId)
      navigate('/dashboard')
    } catch {
      setError('Signup failed. Slug may already exist or password too short.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-lg">
        <CardHeader>Create Workspace</CardHeader>
        <CardContent>
          <form className="grid gap-3 md:grid-cols-2" onSubmit={onSubmit}>
            <div className="md:col-span-2">
              <Label htmlFor="tenantName">Company Name</Label>
              <Input id="tenantName" name="tenantName" required />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor="tenantSlug">Company Slug</Label>
              <Input id="tenantSlug" name="tenantSlug" required />
            </div>
            <div>
              <Label htmlFor="firstName">First Name</Label>
              <Input id="firstName" name="firstName" required />
            </div>
            <div>
              <Label htmlFor="lastName">Last Name</Label>
              <Input id="lastName" name="lastName" required />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor="email">Work Email</Label>
              <Input id="email" name="email" type="email" required />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor="password">Password</Label>
              <Input id="password" name="password" type="password" minLength={8} required />
            </div>
            {error && <p className="text-sm text-red-600 md:col-span-2">{error}</p>}
            <div className="md:col-span-2">
              <Button type="submit" className="w-full" disabled={loading}>{loading ? 'Creating...' : 'Create Workspace'}</Button>
            </div>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            Already have one? <Link className="underline" to="/login">Login</Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
