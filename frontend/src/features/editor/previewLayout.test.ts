import { describe, expect, it } from 'vitest'
import type { PatternPiece } from '@/shared/api/types'
import { layoutPieces, toPath } from './previewLayout'

function piece(code: string, w: number, h: number): PatternPiece {
  return {
    id: code, code, name: code, material: 'Tecido principal', cutQuantity: 1, cutOnFold: false, mirrored: false,
    seamLine: [], cutLine: [[-10, -10], [w - 10, -10], [w - 10, h - 10], [-10, h - 10]], foldLines: [],
    grainline: { from: [0, 0], to: [0, 1] }, notches: [], internalLines: [], edges: [], labelAnchor: [0, 0],
    bounds: { minX: -10, minY: -10, maxX: w - 10, maxY: h - 10 },
  }
}

describe('layoutPieces', () => {
  it('não sobrepõe peças e cabe no tamanho calculado', () => {
    const pieces = [piece('A', 300, 600), piece('B', 280, 580), piece('C', 400, 250), piece('D', 500, 40)]
    const { placed, width, height } = layoutPieces(pieces, 40)
    const boxes = placed.map(({ piece: p, dx, dy }) => ({
      x0: p.bounds.minX + dx, y0: p.bounds.minY + dy, x1: p.bounds.maxX + dx, y1: p.bounds.maxY + dy,
    }))
    for (const b of boxes) {
      expect(b.x0).toBeGreaterThanOrEqual(0)
      expect(b.y0).toBeGreaterThanOrEqual(0)
      expect(b.x1).toBeLessThanOrEqual(width)
      expect(b.y1).toBeLessThanOrEqual(height)
    }
    for (let i = 0; i < boxes.length; i++)
      for (let j = i + 1; j < boxes.length; j++) {
        const a = boxes[i], b = boxes[j]
        const overlap = a.x0 < b.x1 && b.x0 < a.x1 && a.y0 < b.y1 && b.y0 < a.y1
        expect(overlap).toBe(false)
      }
    expect(placed.map((p) => p.piece.code)).toEqual(['A', 'B', 'C', 'D'])
  })
})

it('toPath gera caminho SVG fechado', () => {
  expect(toPath([[0, 0], [10, 0], [10, 5]], true, 1, 2)).toBe('M1.0 2.0 L11.0 2.0 L11.0 7.0 Z')
})
