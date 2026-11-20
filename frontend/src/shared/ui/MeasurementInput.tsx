import { useId, useState } from 'react'
import type { MeasurementGuide } from '@/shared/api/types'
import { formatCm, parseCm, type MeasurementIssue } from '@/shared/lib/measurements'

const issueText: Record<MeasurementIssue, string> = {
  missing: 'Obrigatória para este modelo',
  outOfRange: 'Fora do intervalo plausível',
  invalid: 'Número inválido',
}

/**
 * Campo de medida em cm, editável com vírgula ou ponto. Mantém o texto digitado
 * enquanto o usuário edita e só propaga números (ou undefined quando vazio).
 */
export function MeasurementInput({
  guide,
  value,
  onChange,
  issue,
  required,
  compact,
}: {
  guide: MeasurementGuide
  value: number | undefined
  onChange: (v: number | undefined) => void
  issue?: MeasurementIssue
  required?: boolean
  compact?: boolean
}) {
  const id = useId()
  const [text, setText] = useState(formatCm(value))
  const [prevValue, setPrevValue] = useState(value)

  // valor mudou por fora (carregar perfil/tamanho padrão): ajusta o texto durante o render,
  // sem efeito — mas preserva o que o usuário digitou se já representa o mesmo número ("72," vs 72)
  if (!sameNumber(prevValue, value)) {
    setPrevValue(value)
    if (!sameNumber(parseCm(text), value)) setText(formatCm(value))
  }

  return (
    <div>
      <label htmlFor={id} className="mb-1 flex items-baseline justify-between gap-2 text-sm font-medium text-ink-2">
        <span>
          {guide.label}
          {required && <span className="text-accent" aria-hidden> *</span>}
        </span>
        <span className="text-xs font-normal text-muted">cm</span>
      </label>
      <input
        id={id}
        inputMode="decimal"
        autoComplete="off"
        value={text}
        aria-invalid={issue ? true : undefined}
        aria-describedby={`${id}-help`}
        placeholder={`${guide.minCm}–${guide.maxCm}`}
        onChange={(e) => {
          setText(e.target.value)
          onChange(parseCm(e.target.value))
        }}
        className="w-full rounded-lg border border-line bg-white px-3 py-2 text-sm tabular-nums text-ink placeholder:text-muted/60 focus:border-ink-2 focus:outline-none aria-[invalid=true]:border-danger"
      />
      <p id={`${id}-help`} className={`mt-1 text-xs ${issue ? 'text-danger' : 'text-muted'}`}>
        {issue ? issueText[issue] : compact ? '' : guide.howToMeasure}
      </p>
    </div>
  )
}

function sameNumber(a: number | undefined, b: number | undefined): boolean {
  return a === b || (a !== undefined && b !== undefined && Number.isNaN(a) && Number.isNaN(b))
}
