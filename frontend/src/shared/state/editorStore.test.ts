import { describe, expect, it } from 'vitest'
import { editorStore, initialDraft } from './editorStore'

describe('editorStore', () => {
  it('aplica patches, notifica e persiste na sessão', () => {
    editorStore.reset()
    let calls = 0
    const unsub = editorStore.subscribe(() => calls++)
    editorStore.set({ garmentType: 'SAIA_GODE' })
    editorStore.set((s) => ({ measurements: { ...s.measurements, waist: 72 } }))
    unsub()
    expect(calls).toBe(2)
    expect(editorStore.get()).toMatchObject({ garmentType: 'SAIA_GODE', measurements: { waist: 72 } })
    expect(JSON.parse(sessionStorage.getItem('moldelab.editor.v1')!)).toMatchObject({ garmentType: 'SAIA_GODE' })
  })

  it('reset volta ao inicial', () => {
    editorStore.set({ projectName: 'x' })
    editorStore.reset()
    expect(editorStore.get()).toEqual(initialDraft)
  })
})
