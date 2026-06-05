import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getDashboard } from '../api/api'
import { Card, CardContent, CardHeader } from '../components/ui/card'

export function DashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ['dashboard'], queryFn: getDashboard })

  if (isLoading || !data) return <div>Loading dashboard...</div>

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Dashboard</h1>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Link to="/projects"><Card><CardContent className="pt-4"><div className="text-sm text-muted-foreground">Projects</div><div className="text-2xl font-semibold">{data.projectCount}</div></CardContent></Card></Link>
        <Link to="/users"><Card><CardContent className="pt-4"><div className="text-sm text-muted-foreground">Users</div><div className="text-2xl font-semibold">{data.userCount}</div></CardContent></Card></Link>
        <Link to="/tasks"><Card><CardContent className="pt-4"><div className="text-sm text-muted-foreground">Open Tasks</div><div className="text-2xl font-semibold">{data.openTaskCount}</div></CardContent></Card></Link>
        <Link to="/tasks"><Card><CardContent className="pt-4"><div className="text-sm text-muted-foreground">Completed</div><div className="text-2xl font-semibold">{data.completedTaskCount}</div></CardContent></Card></Link>
      </div>

      <Card>
        <CardHeader>Recent Activity</CardHeader>
        <CardContent className="space-y-2 text-sm">
          {data.recentActivity.length === 0 && <div className="text-muted-foreground">No activity yet</div>}
          {data.recentActivity.map((a) => (
            <div key={a.id}>• {a.actorName} {a.action} {a.entityType}</div>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>My Tasks</CardHeader>
        <CardContent className="space-y-2 text-sm">
          {data.myTasks.length === 0 && <div className="text-muted-foreground">No tasks assigned to you</div>}
          {data.myTasks.map((t) => (
            <div key={t.id}>□ {t.title} <span className="text-muted-foreground">({t.projectName})</span></div>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
