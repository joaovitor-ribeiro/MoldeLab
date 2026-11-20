import { describe, expect, it } from 'vitest'
import type { MeasurementGuide } from '@/shared/api/types'
import { cleanMeasurements, formatCm, parseCm, validateMeasurements } from './measurements'

const guide: MeasurementGuide[] = [
  { key: 'WAIST', jsonName: 'waist', label: 'Cintura', howToMeasure: '', minCm: 45, maxCm: 170 },
  { key: 'HIP', jsonName: 'hip', label: 'Quadril', howToMeasure: '', minCm: 65, maxCm: 190 },
  { key: 'BUST', jsonName: 'bust', label: 'Busto', howToMeasure: '', minCm: 60, maxCm: 180 },
]

describe('parseCm', () => {
  it('aceita vírgula e ponto', () => {
    expect(parseCm('72,5')).toBe(72.5)
    expect(parseCm(' 72.5 ')).toBe(72.5)
  })
  it('vazio vira undefined e lixo vira NaN', () => {
    expect(parseCm('')).toBeUndefined()
    expect(parseCm('abc')).toBeNaN()
  })
  it('formata com vírgula', () => {
    expect(formatCm(72.5)).toBe('72,5')
    expect(formatCm(undefined)).toBe('')
  })
})

describe('validateMeasurements', () => {
  it('marca obrigatórias ausentes, fora do intervalo e inválidas', () => {
    const issues = validateMeasurements({ waist: 7, bust: Number.NaN }, ['waist', 'hip'], guide)
    expect(issues).toEqual({ waist: 'outOfRange', hip: 'missing', bust: 'invalid' })
  })
  it('opcional ausente não é problema', () => {
    expect(validateMeasurements({ waist: 70, hip: 98 }, ['waist', 'hip'], guide)).toEqual({})
  })
})

it('cleanMeasurements remove vazios e NaN', () => {
  expect(cleanMeasurements({ waist: 70, hip: undefined, bust: Number.NaN })).toEqual({ waist: 70 })
})
