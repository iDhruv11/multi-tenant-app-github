import * as React from 'react'
import { cn } from '../../lib/utils'

export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        'flex h-9 w-full rounded-md border border-border bg-white px-3 py-1 text-sm outline-none focus:ring-1 focus:ring-slate-400',
        className
      )}
      {...props}
    />
  )
)
Input.displayName = 'Input'
