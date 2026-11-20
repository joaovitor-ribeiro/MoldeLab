import { useSyncExternalStore } from 'react'
import type { GarmentType, Measurements, Seams } from '@/shared/api/types'

/**
 * Rascunho do molde em edição. Compartilhado entre "Novo molde", "Projetos" e o Editor,
 * persistido em sessionStorage para sobreviver a recarregamentos.
 */
export interface EditorDraft {
  garmentType: GarmentType | null
  params: Record<string, number>
  measurements: Measurements
  profileId: string | null
  profileName: string | null
  seams: Required<Seams>
  fabricWidthCm: number
  sketchDataUrl: string | null
  projectId: string | null
  projectName: string
  aiNotes: string[]
  fabricColor: string
}

export const DEFAULT_SEAMS: Required<Seams> = { seamCm: 1, hemCm: 2.5, finishCm: 0.7 }

export const initialDraft: EditorDraft = {
  garmentType: null,
  params: {},
  measurements: {},
  profileId: null,
  profileName: null,
  seams: DEFAULT_SEAMS,
  fabricWidthCm: 150,
  sketchDataUrl: null,
  projectId: null,
  projectName: '',
  aiNotes: [],
  fabricColor: '#c2552d',
}

const KEY = 'moldelab.editor.v1'
const listeners = new Set<() => void>()

function load(): EditorDraft {
  try {
    const raw = sessionStorage.getItem(KEY)
    return raw ? { ...initialDraft, ...(JSON.parse(raw) as Partial<EditorDraft>) } : initialDraft
  } catch {
    return initialDraft
  }
}

let state: EditorDraft = typeof sessionStorage === 'undefined' ? initialDraft : load()

function persist() {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(state))
  } catch {
    // imagem grande demais para o sessionStorage: mantém só em memória
    try {
      sessionStorage.setItem(KEY, JSON.stringify({ ...state, sketchDataUrl: null }))
    } catch {
      /* ignora */
    }
  }
}

export const editorStore = {
  get: () => state,
  set(patch: Partial<EditorDraft> | ((s: EditorDraft) => Partial<EditorDraft>)) {
    const p = typeof patch === 'function' ? patch(state) : patch
    state = { ...state, ...p }
    persist()
    listeners.forEach((l) => l())
  },
  reset(patch: Partial<EditorDraft> = {}) {
    state = { ...initialDraft, ...patch }
    persist()
    listeners.forEach((l) => l())
  },
  subscribe(l: () => void) {
    listeners.add(l)
    return () => listeners.delete(l)
  },
}

export function useEditorDraft(): EditorDraft {
  return useSyncExternalStore(editorStore.subscribe, editorStore.get, editorStore.get)
}
