import { NavLink, Outlet } from 'react-router-dom'

const links = [
  { to: '/novo', label: 'Novo molde' },
  { to: '/editor', label: 'Editor' },
  { to: '/medidas', label: 'Medidas' },
  { to: '/projetos', label: 'Projetos' },
]

export function Layout() {
  return (
    <div className="min-h-dvh">
      <a href="#conteudo" className="sr-only focus:not-sr-only focus:fixed focus:left-2 focus:top-2 focus:z-50 focus:rounded focus:bg-card focus:px-3 focus:py-2">
        Pular para o conteúdo
      </a>
      <header className="sticky top-0 z-30 border-b border-line bg-paper/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center gap-6 px-4 py-3">
          <NavLink to="/" className="flex items-center gap-2" aria-label="MoldeLab, início">
            <img src="/favicon.svg" alt="" className="size-7" />
            <span className="font-display text-xl font-semibold tracking-tight">MoldeLab</span>
          </NavLink>
          <nav aria-label="Principal" className="flex flex-wrap gap-1 text-sm">
            {links.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                className={({ isActive }) =>
                  `rounded-md px-3 py-1.5 font-medium transition-colors ${isActive ? 'bg-ink text-paper' : 'text-ink-2 hover:bg-paper-2'}`
                }
              >
                {l.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>
      <main id="conteudo" className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
