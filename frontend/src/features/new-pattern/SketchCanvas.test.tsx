import { act, fireEvent, render, screen } from '@testing-library/react'
import { createRef } from 'react'
import { expect, it } from 'vitest'
import { SketchCanvas, type SketchHandle } from './SketchCanvas'

it('traço rápido (down → moves → up no mesmo tick) não quebra e registra o desenho', () => {
  const ref = createRef<SketchHandle>()
  render(<SketchCanvas ref={ref} />)
  const canvas = screen.getByLabelText('Área de desenho da roupa')
  expect(ref.current!.isEmpty()).toBe(true)
  act(() => {
    fireEvent.pointerDown(canvas, { clientX: 10, clientY: 10, pointerId: 1 })
    for (let i = 1; i <= 10; i++) fireEvent.pointerMove(canvas, { clientX: 10 + i, clientY: 10 + i, pointerId: 1 })
    fireEvent.pointerUp(canvas, { pointerId: 1 })
  })
  expect(ref.current!.isEmpty()).toBe(false)
  // desfazer remove o traço
  act(() => fireEvent.click(screen.getByRole('button', { name: 'Desfazer' })))
  expect(ref.current!.isEmpty()).toBe(true)
})
