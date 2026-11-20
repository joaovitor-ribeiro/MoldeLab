import type { MeasurementGuide, MeasurementName, Measurements } from '@/shared/api/types'

export const MEASUREMENT_GROUPS: { title: string; names: MeasurementName[] }[] = [
  { title: 'Geral e tronco', names: ['height', 'bust', 'waist', 'hip', 'neck', 'shoulderWidth', 'backWaistLength'] },
  { title: 'Braços', names: ['armLength', 'upperArm', 'wrist'] },
  { title: 'Da cintura para baixo', names: ['waistToHip', 'waistToKnee', 'waistToFloor', 'thigh', 'crotchDepth'] },
]

/** Converte texto digitado (aceita vírgula) em número. Vazio → undefined; inválido → NaN. */
export function parseCm(text: string | number | undefined | null): number | undefined {
  if (text === undefined || text === null) return undefined
  if (typeof text === 'number') return Number.isFinite(text) ? text : Number.NaN
  const t = text.trim().replace(',', '.')
  if (t === '') return undefined
  const n = Number(t)
  return Number.isFinite(n) ? n : Number.NaN
}

export function formatCm(v: number | undefined): string {
  if (v === undefined || Number.isNaN(v)) return ''
  return String(Math.round(v * 10) / 10).replace('.', ',')
}

export type MeasurementIssue = 'missing' | 'outOfRange' | 'invalid'

/** Problemas por medida, considerando as obrigatórias do modelo e os limites do guia. */
export function validateMeasurements(
  values: Measurements,
  required: MeasurementName[],
  guide: MeasurementGuide[],
): Partial<Record<MeasurementName, MeasurementIssue>> {
  const issues: Partial<Record<MeasurementName, MeasurementIssue>> = {}
  for (const g of guide) {
    const v = values[g.jsonName] as number | undefined | null // null pode vir do sessionStorage (NaN serializado)
    if (v === undefined || v === null) {
      if (required.includes(g.jsonName)) issues[g.jsonName] = 'missing'
    } else if (Number.isNaN(v)) issues[g.jsonName] = 'invalid'
    else if (v < g.minCm || v > g.maxCm) issues[g.jsonName] = 'outOfRange'
  }
  return issues
}

export function cleanMeasurements(values: Measurements): Measurements {
  const out: Measurements = {}
  for (const [k, v] of Object.entries(values) as [MeasurementName, number | undefined][]) {
    if (v !== undefined && Number.isFinite(v)) out[k] = v
  }
  return out
}
