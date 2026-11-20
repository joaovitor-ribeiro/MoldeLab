// Tipos espelhando docs/api-contracts.md. Medidas e parâmetros em cm; geometria em mm.

export const MEASUREMENT_NAMES = [
  'height',
  'bust',
  'waist',
  'hip',
  'neck',
  'shoulderWidth',
  'backWaistLength',
  'armLength',
  'upperArm',
  'wrist',
  'waistToHip',
  'waistToKnee',
  'waistToFloor',
  'thigh',
  'crotchDepth',
] as const

export type MeasurementName = (typeof MEASUREMENT_NAMES)[number]
export type Measurements = Partial<Record<MeasurementName, number>>

export type GarmentType = 'SAIA_RETA' | 'SAIA_GODE' | 'CAMISETA'

export type Point = [number, number]

export interface ParamSpec {
  key: string
  label: string
  unit: string
  min: number
  max: number
  defaultValue: number
  step: number
  help: string
  options: { value: number; label: string }[]
}

export interface CatalogItem {
  type: GarmentType
  label: string
  description: string
  requiredMeasurements: { key: string; jsonName: MeasurementName; label: string }[]
  params: ParamSpec[]
}

export interface MeasurementGuide {
  key: string
  jsonName: MeasurementName
  label: string
  howToMeasure: string
  minCm: number
  maxCm: number
}

export interface StandardSize {
  id: string
  label: string
  group: string
  measurements: Measurements
}

export interface Seams {
  seamCm?: number
  hemCm?: number
  finishCm?: number
}

export interface Segment {
  from: Point
  to: Point
}

export interface Bounds {
  minX: number
  minY: number
  maxX: number
  maxY: number
}

export interface PatternPiece {
  id: string
  code: string
  name: string
  material: string
  cutQuantity: number
  cutOnFold: boolean
  mirrored: boolean
  seamLine: Point[]
  cutLine: Point[]
  foldLines: Point[][]
  grainline: Segment
  notches: Segment[]
  internalLines: Point[][]
  edges: { name: string; kind: 'SEAM' | 'HEM' | 'FOLD' | 'FINISH'; lengthMm: number }[]
  labelAnchor: Point
  bounds: Bounds
}

export interface Pattern {
  garmentType: GarmentType
  garmentLabel: string
  params: Record<string, number>
  seams: { seamMm: number; hemMm: number; finishMm: number }
  measurements: Measurements
  pieces: PatternPiece[]
  fabric: { material: string; fabricWidthMm: number; lengthMm: number }[]
  warnings: string[]
}

export interface DraftRequest {
  garmentType: GarmentType
  measurements: Measurements
  params: Record<string, number>
  seams?: Seams
  fabricWidthCm?: number
}

export type PaperSize = 'A4' | 'LETTER' | 'A3' | 'A0' | 'PLOTTER_914' | 'PLOTTER_1067'

export interface PdfRequest {
  draft: DraftRequest
  paper: PaperSize
  title?: string
  profileName?: string
  showSeamLine?: boolean
}

export interface Profile {
  id: string
  name: string
  notes?: string
  measurements: Measurements
  createdAt: string
  updatedAt: string
}

export interface ProjectSummary {
  id: string
  name: string
  garmentType: GarmentType
  hasSketch: boolean
  updatedAt: string
}

export interface Project {
  id: string
  name: string
  garmentType: GarmentType
  profileId?: string
  measurements: Measurements
  params: Record<string, number>
  seams: Seams
  sketchDataUrl?: string
  createdAt: string
  updatedAt: string
}

export interface ProjectRequest {
  name: string
  garmentType: GarmentType
  profileId?: string
  measurements: Measurements
  params: Record<string, number>
  seams?: Seams
  sketchDataUrl?: string
}

export interface AiStatus {
  enabled: boolean
  model?: string
}

export interface GarmentSuggestion {
  garmentType?: GarmentType
  supported: boolean
  confidence: number
  description?: string
  params: Record<string, number>
  notes: string[]
}
