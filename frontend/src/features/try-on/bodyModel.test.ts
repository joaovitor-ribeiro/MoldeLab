import { describe, expect, it } from 'vitest'
import type { GarmentType } from '@/shared/api/types'
import { bodyRadiusAt, buildBody, buildGarment, DEPTH, radiusFromCircumference } from './bodyModel'

function ellipsePerimeter(a: number, b: number) {
  return Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b)))
}

describe('bodyModel', () => {
  it('raio da elipse reproduz a circunferência', () => {
    const a = radiusFromCircumference(100)
    expect(ellipsePerimeter(a, a * DEPTH)).toBeCloseTo(1.0, 6)
  })

  it('alturas do corpo seguem a ordem anatômica', () => {
    const b = buildBody({ height: 170, waistToFloor: 106 })
    const order = [b.floor, b.knee, b.crotch, b.hip, b.waist, b.bust, b.armpit, b.shoulder, b.neck]
    for (let i = 1; i < order.length; i++) expect(order[i]).toBeGreaterThan(order[i - 1])
    expect(b.neck + b.headRadius * 2).toBeLessThanOrEqual(b.height + 0.1)
  })

  it('medidas ausentes usam o padrão e inválidas são ignoradas', () => {
    const b = buildBody({ waist: Number.NaN, hip: -3 })
    expect(b.m.waist).toBe(74)
    expect(b.m.hip).toBe(100)
  })

  it.each<[GarmentType, Record<string, number>]>([
    ['SAIA_RETA', { length: 60, flare: 5 }],
    ['SAIA_GODE', { length: 70, fraction: 1 }],
    ['CAMISETA', { lengthBelowWaist: 50, sleeveLength: 20 }],
  ])('roupa %s fica por fora do corpo e com anéis de cima para baixo', (type, params) => {
    const body = buildBody({})
    const g = buildGarment(type, params, body)
    for (let i = 1; i < g.rings.length; i++) expect(g.rings[i].y).toBeLessThan(g.rings[i - 1].y)
    for (const ring of g.rings) {
      if (ring.y > body.crotch && ring.y < body.shoulder - 0.04) {
        expect(ring.r).toBeGreaterThanOrEqual(bodyRadiusAt(body, ring.y))
      }
    }
  })

  it('comprimento da saia muda a barra', () => {
    const body = buildBody({})
    const short = buildGarment('SAIA_RETA', { length: 45 }, body)
    const long = buildGarment('SAIA_RETA', { length: 90 }, body)
    expect(long.rings.at(-1)!.y).toBeLessThan(short.rings.at(-1)!.y)
  })

  it('regata não tem mangas', () => {
    expect(buildGarment('CAMISETA', { sleeveLength: 0 }, buildBody({})).sleeves).toBeUndefined()
  })
})
