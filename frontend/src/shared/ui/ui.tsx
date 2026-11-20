import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'

const variants: Record<Variant, string> = {
  primary: 'bg-ink text-paper hover:bg-ink-2 disabled:bg-muted',
  secondary: 'bg-card text-ink border border-line hover:border-ink-2 disabled:text-muted',
  ghost: 'text-ink-2 hover:bg-paper-2 disabled:text-muted',
  danger: 'bg-card text-danger border border-line hover:border-danger',
}

export function Button({
  variant = 'primary',
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return (
    <button
      type="button"
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed ${variants[variant]} ${className}`}
      {...props}
    />
  )
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`rounded-[var(--radius-card)] border border-line bg-card shadow-[var(--shadow-card)] ${className}`}>{children}</section>
}

export function SectionTitle({ children, hint }: { children: ReactNode; hint?: ReactNode }) {
  return (
    <div className="mb-3">
      <h2 className="text-lg font-semibold">{children}</h2>
      {hint && <p className="text-sm text-muted">{hint}</p>}
    </div>
  )
}

export function Label({ htmlFor, children }: { htmlFor: string; children: ReactNode }) {
  return (
    <label htmlFor={htmlFor} className="mb-1 block text-sm font-medium text-ink-2">
      {children}
    </label>
  )
}

export function TextInput({ className = '', ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={`w-full rounded-lg border border-line bg-white px-3 py-2 text-sm text-ink placeholder:text-muted focus:border-ink-2 focus:outline-none aria-[invalid=true]:border-danger ${className}`}
      {...props}
    />
  )
}

export function Select({ className = '', children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={`w-full rounded-lg border border-line bg-white px-3 py-2 text-sm text-ink focus:border-ink-2 focus:outline-none ${className}`}
      {...props}
    >
      {children}
    </select>
  )
}

export function Banner({ tone = 'info', children }: { tone?: 'info' | 'warn' | 'error' | 'ok'; children: ReactNode }) {
  const tones = {
    info: 'border-fold/30 bg-fold/5 text-ink-2',
    warn: 'border-tape/60 bg-tape/10 text-ink-2',
    error: 'border-danger/40 bg-danger/5 text-danger',
    ok: 'border-ok/40 bg-ok/5 text-ok',
  }
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`rounded-lg border px-3 py-2 text-sm ${tones[tone]}`}>
      {children}
    </div>
  )
}

export function Spinner({ label = 'Carregando' }: { label?: string }) {
  return (
    <span className="inline-flex items-center gap-2 text-sm text-muted" role="status">
      <span className="size-4 animate-spin rounded-full border-2 border-line border-t-accent" aria-hidden />
      {label}
    </span>
  )
}

export function Badge({ children }: { children: ReactNode }) {
  return <span className="rounded-full border border-line bg-paper-2 px-2 py-0.5 text-xs font-medium text-ink-2">{children}</span>
}
