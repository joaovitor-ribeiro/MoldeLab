import type { PatternPiece, Point } from '@/shared/api/types'

export interface PlacedPiece {
  piece: PatternPiece
  dx: number
  dy: number
}

export interface PreviewLayout {
  placed: PlacedPiece[]
  width: number
  height: number
}

/** Distribui as peças lado a lado (quebrando linhas) para a prévia. Unidades em mm. */
export function layoutPieces(pieces: PatternPiece[], gap = 40): PreviewLayout {
  if (pieces.length === 0) return { placed: [], width: 100, height: 100 }
  const sorted = [...pieces].sort((a, b) => h(b) - h(a))
  const totalArea = pieces.reduce((s, p) => s + (w(p) + gap) * (h(p) + gap), 0)
  const maxW = Math.max(...pieces.map(w))
  const rowWidth = Math.max(maxW + 2 * gap, Math.sqrt(totalArea) * 1.7)

  const placed: PlacedPiece[] = []
  let x = gap
  let y = gap
  let rowH = 0
  let width = 0
  for (const p of sorted) {
    if (x + w(p) + gap > rowWidth && x > gap) {
      x = gap
      y += rowH + gap
      rowH = 0
    }
    placed.push({ piece: p, dx: x - p.bounds.minX, dy: y - p.bounds.minY })
    x += w(p) + gap
    width = Math.max(width, x)
    rowH = Math.max(rowH, h(p))
  }
  // mantém a ordem original (A, B, C...) para leitores de tela
  placed.sort((a, b) => a.piece.code.localeCompare(b.piece.code))
  return { placed, width, height: y + rowH + gap }
}

function w(p: PatternPiece) {
  return p.bounds.maxX - p.bounds.minX
}
function h(p: PatternPiece) {
  return p.bounds.maxY - p.bounds.minY
}

export function toPath(points: Point[], close: boolean, dx = 0, dy = 0): string {
  if (points.length === 0) return ''
  const d = points.map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${(x + dx).toFixed(1)} ${(y + dy).toFixed(1)}`).join(' ')
  return close ? d + ' Z' : d
}
