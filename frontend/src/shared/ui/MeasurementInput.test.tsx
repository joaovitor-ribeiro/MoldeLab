import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import type { MeasurementGuide } from '@/shared/api/types'
import { MeasurementInput } from './MeasurementInput'

const guide: MeasurementGuide = { key: 'WAIST', jsonName: 'waist', label: 'Cintura', howToMeasure: 'Na parte mais fina', minCm: 45, maxCm: 170 }

describe('MeasurementInput', () => {
  it('edita valor com vírgula e propaga número', async () => {
    const onChange = vi.fn()
    function Host() {
      const [v, setV] = useState<number | undefined>(70)
      return <MeasurementInput guide={guide} value={v} onChange={(x) => { setV(x); onChange(x) }} />
    }
    render(<Host />)
    const input = screen.getByLabelText(/Cintura/)
    expect(input).toHaveValue('70')
    await userEvent.clear(input)
    await userEvent.type(input, '72,5')
    expect(onChange).toHaveBeenLastCalledWith(72.5)
    expect(input).toHaveValue('72,5') // mantém o que foi digitado
  })

  it('mostra o problema e marca como inválido', () => {
    render(<MeasurementInput guide={guide} value={7} issue="outOfRange" onChange={() => {}} />)
    expect(screen.getByLabelText(/Cintura/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Fora do intervalo plausível')).toBeInTheDocument()
  })

  it('atualiza quando o valor muda por fora (carregar perfil)', () => {
    const { rerender } = render(<MeasurementInput guide={guide} value={70} onChange={() => {}} />)
    rerender(<MeasurementInput guide={guide} value={81.5} onChange={() => {}} />)
    expect(screen.getByLabelText(/Cintura/)).toHaveValue('81,5')
  })
})
