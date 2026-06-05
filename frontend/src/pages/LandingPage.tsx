import { Link } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { Card, CardContent, CardHeader } from '../components/ui/card'

export function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      <header className="mx-auto flex max-w-5xl items-center justify-between px-4 py-6">
        <div className="text-lg font-semibold">Multi-Tenant SaaS</div>
        <div className="flex gap-2">
          <Link to="/login"><Button variant="outline">Login</Button></Link>
          <Link to="/register"><Button>Start Free</Button></Link>
        </div>
      </header>

      <section className="mx-auto max-w-5xl px-4 py-16 text-center">
        <h1 className="text-3xl font-bold md:text-4xl">Manage Projects Across Your Organization</h1>
        <p className="mt-3 text-muted-foreground">Secure Multi-Tenant Workspace · Built For Teams</p>
        <div className="mt-6 flex justify-center gap-3">
          <Link to="/register"><Button>Start Free</Button></Link>
          <Link to="/login"><Button variant="outline">Login</Button></Link>
        </div>
      </section>

      <section className="mx-auto grid max-w-5xl gap-4 px-4 pb-16 md:grid-cols-3">
        <Card>
          <CardHeader className="font-semibold">Secure Workspaces</CardHeader>
          <CardContent className="text-sm text-muted-foreground">Each company receives isolated data.</CardContent>
        </Card>
        <Card>
          <CardHeader className="font-semibold">Project Management</CardHeader>
          <CardContent className="text-sm text-muted-foreground">Projects, members, and tasks.</CardContent>
        </Card>
        <Card>
          <CardHeader className="font-semibold">Audit Logging</CardHeader>
          <CardContent className="text-sm text-muted-foreground">Track important actions across the workspace.</CardContent>
        </Card>
      </section>
    </div>
  )
}
