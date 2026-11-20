import { Link } from 'react-router-dom'
import { Card } from '@/shared/ui/ui'

const steps = [
  { n: '1', title: 'Medidas', text: 'Cadastre e edite suas medidas (ou parta de um tamanho padrão).' },
  { n: '2', title: 'Desenho ou foto', text: 'Desenhe a roupa ou envie uma foto; a IA sugere o modelo e os ajustes.' },
  { n: '3', title: 'Ajuste e prove', text: 'Ajuste comprimento, evasê, manga… e veja no provador 3D.' },
  { n: '4', title: 'Imprima e corte', text: 'Baixe o PDF em tamanho real, monte as folhas, corte e costure.' },
]

export function HomePage() {
  return (
    <div className="space-y-10 py-6">
      <section className="grid items-center gap-8 md:grid-cols-[1.2fr_1fr]">
        <div>
          <p className="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-accent">Ateliê digital</p>
          <h1 className="text-4xl font-semibold leading-tight md:text-5xl">
            Do desenho ao molde,
            <br />
            <span className="italic text-accent">nas suas medidas.</span>
          </h1>
          <p className="mt-4 max-w-xl text-ink-2">
            Desenhe ou fotografe a roupa que você quer. O MoldeLab gera o molde com margens de costura, piques e fio, em PDF
            na escala real para imprimir em folhas A4 ou plotter.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/novo" className="rounded-lg bg-ink px-5 py-2.5 text-sm font-medium text-paper hover:bg-ink-2">
              Começar um molde
            </Link>
            <Link to="/medidas" className="rounded-lg border border-line bg-card px-5 py-2.5 text-sm font-medium hover:border-ink-2">
              Minhas medidas
            </Link>
          </div>
        </div>
        <HeroIllustration />
      </section>

      <section aria-labelledby="como-funciona">
        <h2 id="como-funciona" className="mb-4 text-2xl font-semibold">Como funciona</h2>
        <ol className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((s) => (
            <li key={s.n}>
              <Card className="h-full p-5">
                <span className="font-display text-3xl text-accent">{s.n}</span>
                <h3 className="mt-1 text-lg font-semibold">{s.title}</h3>
                <p className="mt-1 text-sm text-ink-2">{s.text}</p>
              </Card>
            </li>
          ))}
        </ol>
      </section>
    </div>
  )
}

function HeroIllustration() {
  return (
    <svg viewBox="0 0 360 300" className="w-full" role="img" aria-label="Ilustração de molde de saia sobre papel">
      <rect x="10" y="10" width="340" height="280" rx="14" fill="#fffdf8" stroke="#ddd2bf" />
      <g stroke="#1f1b16" fill="none" strokeWidth="2">
        <path d="M60 50 Q120 44 150 40 L175 120 L195 260 L60 260 Z" />
        <path d="M70 58 Q120 52 144 49 L167 122 L186 250 L70 250" strokeDasharray="6 4" stroke="#7a7064" strokeWidth="1.2" />
        <path d="M108 52 L112 120 L118 51" strokeDasharray="5 2 1 2" strokeWidth="1.2" />
      </g>
      <line x1="60" y1="40" x2="60" y2="270" stroke="#2f5fb3" strokeWidth="3" strokeDasharray="10 4 2 4" />
      <g stroke="#1f1b16" fill="none" strokeWidth="2">
        <path d="M215 60 L290 60 Q300 110 330 140 L330 260 L215 260 Z" />
      </g>
      <path d="M248 120 v90 m-4 -84 l4 -6 4 6 m-8 78 l4 6 4 -6" stroke="#1f1b16" strokeWidth="1.5" fill="none" />
      <rect x="220" y="30" width="120" height="14" rx="3" fill="#e3b23c" />
      {Array.from({ length: 12 }, (_, i) => (
        <line key={i} x1={226 + i * 9.5} y1="30" x2={226 + i * 9.5} y2={i % 2 ? 36 : 39} stroke="#1f1b16" strokeWidth="1" />
      ))}
      <text x="95" y="170" fontFamily="Fraunces Variable, serif" fontSize="34" fill="#1f1b16">A</text>
      <text x="262" y="200" fontFamily="Fraunces Variable, serif" fontSize="34" fill="#1f1b16">B</text>
    </svg>
  )
}
