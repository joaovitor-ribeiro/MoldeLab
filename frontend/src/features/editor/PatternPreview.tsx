import { useMemo, useState } from 'react'
import type { Pattern, PatternPiece, Segment } from '@/shared/api/types'
import { layoutPieces, toPath } from './previewLayout'

/** Prévia vetorial das peças (mm). Linha cheia = corte; tracejada = costura; azul = dobra. */
export function PatternPreview({ pattern, stale }: { pattern: Pattern; stale?: boolean }) {
  const layout = useMemo(() => layoutPieces(pattern.pieces), [pattern])
  const [showSeam, setShowSeam] = useState(true)
  const scaleBarMm = 100

  return (
    <figure className="relative">
      <div className="mb-2 flex flex-wrap items-center gap-4 text-xs text-ink-2">
        <Legend stroke="#1f1b16" label="Linha de corte" />
        <Legend stroke="#7a7064" dash="5 3" label="Linha de costura" />
        <Legend stroke="#2f5fb3" dash="10 3 2 3" label="Dobra do tecido" />
        <label className="ml-auto flex items-center gap-1.5">
          <input type="checkbox" checked={showSeam} onChange={(e) => setShowSeam(e.target.checked)} /> mostrar costura
        </label>
      </div>
      <svg
        viewBox={`0 0 ${layout.width} ${layout.height + 40}`}
        className={`h-auto max-h-[70vh] w-full rounded-lg border border-line bg-white transition-opacity ${stale ? 'opacity-60' : ''}`}
        role="img"
        aria-label={`Prévia do molde: ${pattern.pieces.map((p) => `${p.code} ${p.name}`).join(', ')}`}
      >
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M0 0 L10 5 L0 10 z" fill="#1f1b16" />
          </marker>
        </defs>
        {layout.placed.map(({ piece, dx, dy }) => (
          <Piece key={piece.id} piece={piece} dx={dx} dy={dy} showSeam={showSeam} />
        ))}
        {/* barra de escala */}
        <g transform={`translate(40 ${layout.height + 10})`}>
          <rect width={scaleBarMm} height="8" fill="#e3b23c" />
          <text x={scaleBarMm + 8} y="8" fontSize="14" fill="#4a433a">10 cm</text>
        </g>
      </svg>
      <figcaption className="sr-only">Peças do molde em escala reduzida.</figcaption>
    </figure>
  )
}

function Piece({ piece, dx, dy, showSeam }: { piece: PatternPiece; dx: number; dy: number; showSeam: boolean }) {
  const [lx, ly] = piece.labelAnchor
  const w = piece.bounds.maxX - piece.bounds.minX
  const h = piece.bounds.maxY - piece.bounds.minY
  // rótulo proporcional à menor dimensão, para caber em peças estreitas (ribana, cós)
  const size = Math.max(8, Math.min(60, w / 6, h / 3))
  return (
    <g>
      <path d={toPath(piece.cutLine, true, dx, dy)} fill="#fffaf0" stroke="#1f1b16" strokeWidth="1.6" />
      {showSeam && <path d={toPath(piece.seamLine, true, dx, dy)} fill="none" stroke="#7a7064" strokeWidth="1" strokeDasharray="6 4" />}
      {piece.internalLines.map((l, i) => (
        <path key={i} d={toPath(l, false, dx, dy)} fill="none" stroke="#4a433a" strokeWidth="1" strokeDasharray="8 3 2 3" />
      ))}
      {piece.foldLines.map((l, i) => (
        <path key={i} d={toPath(l, false, dx, dy)} fill="none" stroke="#2f5fb3" strokeWidth="3" strokeDasharray="12 4 3 4" />
      ))}
      {piece.notches.map((n, i) => (
        <SegmentLine key={i} s={n} dx={dx} dy={dy} width={2.2} />
      ))}
      <line
        x1={piece.grainline.from[0] + dx}
        y1={piece.grainline.from[1] + dy}
        x2={piece.grainline.to[0] + dx}
        y2={piece.grainline.to[1] + dy}
        stroke="#1f1b16"
        strokeWidth="1.4"
        markerStart="url(#arrow)"
        markerEnd="url(#arrow)"
      />
      <text x={lx + dx} y={ly + dy - size * 0.2} textAnchor="middle" fontFamily="Fraunces Variable, serif" fontSize={size} fill="#1f1b16">
        {piece.code}
      </text>
      <text x={lx + dx} y={ly + dy + size * 0.45} textAnchor="middle" fontSize={Math.max(6, size * 0.32)} fill="#4a433a">
        {piece.name}
      </text>
    </g>
  )
}

function SegmentLine({ s, dx, dy, width }: { s: Segment; dx: number; dy: number; width: number }) {
  return <line x1={s.from[0] + dx} y1={s.from[1] + dy} x2={s.to[0] + dx} y2={s.to[1] + dy} stroke="#1f1b16" strokeWidth={width} />
}

function Legend({ stroke, dash, label }: { stroke: string; dash?: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <svg width="26" height="6" aria-hidden>
        <line x1="0" y1="3" x2="26" y2="3" stroke={stroke} strokeWidth="2" strokeDasharray={dash} />
      </svg>
      {label}
    </span>
  )
}
