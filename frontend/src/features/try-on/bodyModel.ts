import type { GarmentType, Measurements } from '@/shared/api/types'

/**
 * Modelo 3D paramétrico (aproximado) do corpo e da roupa, em METROS.
 * Seções do tronco são elipses com profundidade = DEPTH × largura.
 * É uma visualização de proporções — não uma simulação física de tecido.
 */

export const DEPTH = 0.72

/** Medidas padrão (cm) usadas quando o usuário não informou alguma. */
export const DEFAULT_BODY: Required<Measurements> = {
  height: 164,
  bust: 92,
  waist: 74,
  hip: 100,
  neck: 35,
  shoulderWidth: 38.5,
  backWaistLength: 41,
  armLength: 59,
  upperArm: 28.5,
  wrist: 15.8,
  waistToHip: 20.5,
  waistToKnee: 58.8,
  waistToFloor: 104.8,
  thigh: 56,
  crotchDepth: 27,
}

const PERIMETER_FACTOR = Math.PI * (3 * (1 + DEPTH) - Math.sqrt((3 + DEPTH) * (1 + 3 * DEPTH)))

/** Semi-eixo maior (m) de uma elipse com a circunferência informada (cm). */
export function radiusFromCircumference(cm: number): number {
  return cm / 100 / PERIMETER_FACTOR
}

/** Raio de um círculo com a circunferência informada (cm) — para membros. */
export function roundRadius(cm: number): number {
  return cm / 100 / (2 * Math.PI)
}

export interface Ring {
  y: number
  r: number
}

export interface BodyModel {
  height: number
  floor: number
  knee: number
  crotch: number
  hip: number
  waist: number
  bust: number
  armpit: number
  shoulder: number
  neck: number
  headRadius: number
  torso: Ring[] // de baixo para cima
  leg: { x: number; topR: number; kneeR: number; ankleR: number }
  arm: { x: number; y: number; length: number; topR: number; wristR: number; angle: number }
  m: Required<Measurements>
}

export function buildBody(input: Measurements): BodyModel {
  const m = { ...DEFAULT_BODY } as Required<Measurements>
  for (const [k, v] of Object.entries(input)) {
    if (typeof v === 'number' && Number.isFinite(v) && v > 0) (m as Record<string, number>)[k] = v
  }
  const H = m.height / 100
  const waist = Math.min(m.waistToFloor / 100, H * 0.7)
  const hip = waist - m.waistToHip / 100
  const crotch = waist - m.crotchDepth / 100
  const knee = Math.max(0.15, waist - m.waistToKnee / 100)
  const neck = Math.min(waist + m.backWaistLength / 100, H * 0.9)
  const shoulder = neck - 0.035
  const armpit = waist + (neck - waist) * 0.62
  const bust = waist + (neck - waist) * 0.45

  const rBust = radiusFromCircumference(m.bust)
  const rWaist = radiusFromCircumference(m.waist)
  const rHip = radiusFromCircumference(m.hip)
  const rNeck = roundRadius(m.neck)
  const rShoulder = Math.max(rBust * 0.95, (m.shoulderWidth / 100) * 0.42)

  const torso: Ring[] = [
    { y: crotch, r: rHip * 0.82 },
    { y: crotch + (hip - crotch) * 0.5, r: rHip * 0.96 },
    { y: hip, r: rHip },
    { y: hip + (waist - hip) * 0.5, r: (rHip + rWaist) / 2 },
    { y: waist, r: rWaist },
    { y: bust, r: rBust },
    { y: armpit, r: rBust * 0.97 },
    { y: shoulder - 0.03, r: rShoulder },
    { y: shoulder, r: rShoulder * 0.8 },
    { y: neck - 0.01, r: rNeck * 1.35 },
    { y: neck, r: rNeck },
  ]

  return {
    height: H,
    floor: 0,
    knee,
    crotch,
    hip,
    waist,
    bust,
    armpit,
    shoulder,
    neck,
    headRadius: H * 0.064,
    torso,
    leg: {
      x: rHip * 0.45,
      topR: roundRadius(m.thigh),
      kneeR: roundRadius(m.thigh) * 0.62,
      ankleR: roundRadius(m.thigh) * 0.38,
    },
    arm: {
      x: (m.shoulderWidth / 100) / 2 - roundRadius(m.upperArm) * 0.6,
      y: shoulder - 0.025,
      length: m.armLength / 100,
      topR: roundRadius(m.upperArm),
      wristR: roundRadius(m.wrist),
      angle: (10 * Math.PI) / 180,
    },
    m,
  }
}

/** Raio do corpo (eixo maior) numa altura y, interpolando os anéis do tronco. */
export function bodyRadiusAt(body: BodyModel, y: number): number {
  const t = body.torso
  if (y <= t[0].y) return t[0].r
  for (let i = 1; i < t.length; i++) {
    if (y <= t[i].y) {
      const a = t[i - 1], b = t[i]
      return a.r + ((b.r - a.r) * (y - a.y)) / (b.y - a.y)
    }
  }
  return t[t.length - 1].r
}

export interface GarmentShape {
  rings: Ring[] // de cima para baixo
  folds: number // nº de ondas do drapeado
  foldAmp: number // amplitude relativa na barra
  sleeves?: { length: number; topR: number; bottomR: number }
}

const GAP = 0.006 // o tecido fica um pouco afastado da pele

/** Converte modelo + parâmetros (mesmas chaves do catálogo) em anéis 3D. */
export function buildGarment(type: GarmentType, params: Record<string, number>, body: BodyModel): GarmentShape {
  const p = (k: string, d: number) => (Number.isFinite(params[k]) ? params[k] : d)
  const { m } = body

  if (type === 'SAIA_RETA') {
    const length = Math.max(p('length', 55) / 100, m.waistToHip / 100 + 0.05)
    const ease = p('hipEase', 4)
    const flare = p('flare', 0)
    const hem = body.waist - length
    const rHip = radiusFromCircumference(m.hip + ease) + GAP
    const rHem = radiusFromCircumference(m.hip + ease + 4 * flare) + GAP
    return {
      rings: [
        { y: body.waist + 0.01, r: radiusFromCircumference(m.waist + 1) + GAP },
        { y: body.hip, r: rHip },
        { y: hem, r: rHem },
      ],
      folds: flare > 2 ? 10 : 0,
      foldAmp: Math.min(0.08, flare / 200),
    }
  }

  if (type === 'SAIA_GODE') {
    const length = p('length', 55) / 100
    const fraction = p('fraction', 1)
    const spread = 0.12 + 0.45 * fraction // ângulo de abertura (rad) ao cair
    const rWaist = radiusFromCircumference(m.waist + 1) + GAP
    const rings: Ring[] = []
    const steps = 12
    for (let i = 0; i <= steps; i++) {
      const t = i / steps
      const y = body.waist + 0.01 - length * t
      let r = rWaist + length * Math.sin(spread) * Math.pow(t, 0.85)
      if (y > body.crotch) r = Math.max(r, bodyRadiusAt(body, y) + GAP * 2)
      rings.push({ y, r })
    }
    return { rings, folds: Math.round(6 + 10 * fraction), foldAmp: 0.05 + 0.1 * fraction }
  }

  // CAMISETA
  const ease = p('ease', 6)
  const below = p('lengthBelowWaist', 20) / 100
  const sleeve = p('sleeveLength', 20) / 100
  const shaping = p('waistShaping', 20) / 100
  const flare = p('hemFlare', 0)
  const hemY = Math.max(body.waist - below, 0.1)
  const rBust = radiusFromCircumference(m.bust + ease) + GAP
  const rWaistTarget = radiusFromCircumference(m.waist + ease) + GAP
  const rWaist = rBust + (rWaistTarget - rBust) * shaping
  const rHip = Math.max(radiusFromCircumference(m.hip + ease) + GAP, rWaist)
  const rings: Ring[] = [
    { y: body.neck - 0.012, r: roundRadius(m.neck) * 1.45 + p('neckWidening', 1) / 200 },
    { y: body.shoulder - 0.005, r: body.torso[8].r + GAP * 2 },
    { y: body.shoulder - 0.035, r: body.torso[7].r + GAP * 2 },
    { y: body.armpit, r: Math.max(rBust, body.torso[6].r + GAP) },
    { y: body.bust, r: rBust },
  ]
  const add = (y: number, r: number) => {
    if (y < rings[rings.length - 1].y - 0.01) rings.push({ y, r })
  }
  add(body.waist, rWaist)
  if (hemY < body.hip) add(body.hip, rHip)
  const hemBase = hemY < body.hip ? rHip : hemY < body.waist ? rWaist + ((rHip - rWaist) * (body.waist - hemY)) / (body.waist - body.hip) : rWaist
  add(hemY, hemBase + radiusFromCircumference(4 * flare))
  return {
    rings,
    folds: flare > 3 ? 8 : 0,
    foldAmp: Math.min(0.06, flare / 300),
    sleeves:
      sleeve >= 0.01
        ? {
            length: Math.min(sleeve, body.arm.length),
            topR: roundRadius(m.upperArm + 4 + ease * 0.25),
            bottomR:
              roundRadius(m.upperArm + 4) +
              (roundRadius(m.wrist + 5) - roundRadius(m.upperArm + 4)) * Math.min(1, sleeve / body.arm.length),
          }
        : undefined,
  }
}
