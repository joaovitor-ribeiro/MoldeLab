import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react'
import { Button } from '@/shared/ui/ui'

export interface SketchHandle {
  toDataUrl: () => string | null
  isEmpty: () => boolean
}

type Stroke = { points: [number, number][]; width: number; erase: boolean }

/** Prancheta de desenho à mão livre (mouse, dedo ou caneta). */
export const SketchCanvas = forwardRef<SketchHandle>(function SketchCanvas(_, ref) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [strokes, setStrokes] = useState<Stroke[]>([])
  const [width, setWidth] = useState(4)
  const [erase, setErase] = useState(false)
  const drawing = useRef<Stroke | null>(null)

  const redraw = useCallback((list: Stroke[]) => {
    const c = canvasRef.current
    const ctx = c?.getContext('2d')
    if (!c || !ctx) return
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, c.width, c.height)
    // linha de centro como guia (não interfere no desenho)
    ctx.save()
    ctx.strokeStyle = '#efe7d8'
    ctx.setLineDash([8, 8])
    ctx.beginPath()
    ctx.moveTo(c.width / 2, 0)
    ctx.lineTo(c.width / 2, c.height)
    ctx.stroke()
    ctx.restore()
    for (const s of list) {
      ctx.strokeStyle = s.erase ? '#ffffff' : '#1f1b16'
      ctx.lineWidth = s.erase ? s.width * 5 : s.width
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.beginPath()
      s.points.forEach(([x, y], i) => (i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)))
      if (s.points.length === 1) ctx.lineTo(s.points[0][0] + 0.1, s.points[0][1])
      ctx.stroke()
    }
  }, [])

  useEffect(() => redraw(strokes), [strokes, redraw])

  useImperativeHandle(ref, () => ({
    toDataUrl: () => (strokes.some((s) => !s.erase) ? canvasRef.current?.toDataURL('image/png') ?? null : null),
    isEmpty: () => !strokes.some((s) => !s.erase),
  }), [strokes])

  const pos = (e: React.PointerEvent<HTMLCanvasElement>): [number, number] => {
    const c = e.currentTarget
    const r = c.getBoundingClientRect()
    return [((e.clientX - r.left) * c.width) / r.width, ((e.clientY - r.top) * c.height) / r.height]
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <Button variant={erase ? 'secondary' : 'primary'} onClick={() => setErase(false)} aria-pressed={!erase}>Lápis</Button>
        <Button variant={erase ? 'primary' : 'secondary'} onClick={() => setErase(true)} aria-pressed={erase}>Borracha</Button>
        <label className="ml-2 flex items-center gap-2 text-ink-2">
          Espessura
          <input type="range" min={1} max={14} value={width} onChange={(e) => setWidth(Number(e.target.value))} />
        </label>
        <Button variant="ghost" onClick={() => setStrokes((s) => s.slice(0, -1))} disabled={strokes.length === 0}>Desfazer</Button>
        <Button variant="ghost" onClick={() => setStrokes([])} disabled={strokes.length === 0}>Limpar</Button>
      </div>
      <canvas
        ref={canvasRef}
        width={900}
        height={1100}
        aria-label="Área de desenho da roupa"
        className="aspect-[9/11] w-full max-w-xl touch-none rounded-lg border border-line bg-white"
        style={{ cursor: erase ? 'cell' : 'crosshair' }}
        onPointerDown={(e) => {
          try {
            e.currentTarget.setPointerCapture(e.pointerId)
          } catch {
            /* ponteiro sintético/inválido: segue sem captura */
          }
          // snapshot imutável: os updaters do React rodam depois, quando a ref já pode ter mudado
          const stroke: Stroke = { points: [pos(e)], width, erase }
          drawing.current = stroke
          setStrokes((s) => [...s, stroke])
        }}
        onPointerMove={(e) => {
          const current = drawing.current
          if (!current) return
          const next: Stroke = { ...current, points: [...current.points, pos(e)] }
          drawing.current = next
          setStrokes((s) => [...s.slice(0, -1), next])
        }}
        onPointerUp={() => (drawing.current = null)}
        onPointerCancel={() => (drawing.current = null)}
      />
      <p className="text-xs text-muted">
        Dica: desenhe a silhueta de frente, indicando comprimento (joelho, canela…), mangas e decote. Pode escrever observações.
      </p>
    </div>
  )
})
