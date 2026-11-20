import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useId, useState } from 'react'
import { apiSend, errorMessage } from '@/shared/api/client'
import { guideQuery, profilesQuery, standardSizesQuery } from '@/shared/api/queries'
import type { CatalogItem, MeasurementName, Profile } from '@/shared/api/types'
import { cleanMeasurements, MEASUREMENT_GROUPS, type MeasurementIssue } from '@/shared/lib/measurements'
import { editorStore, type EditorDraft } from '@/shared/state/editorStore'
import { MeasurementInput } from '@/shared/ui/MeasurementInput'
import { Banner, Button, Label, Select, TextInput } from '@/shared/ui/ui'

export function ParamsPanel({ item, draft }: { item: CatalogItem; draft: EditorDraft }) {
  return (
    <div className="space-y-5">
      {item.params.map((p) => {
        const value = draft.params[p.key] ?? p.defaultValue
        const set = (v: number) => editorStore.set((s) => ({ params: { ...s.params, [p.key]: v } }))
        return <ParamControl key={p.key} spec={p} value={value} onChange={set} />
      })}
      <Button variant="ghost" onClick={() => editorStore.set({ params: {} })}>Voltar aos valores padrão</Button>
    </div>
  )
}

function ParamControl({
  spec,
  value,
  onChange,
}: {
  spec: CatalogItem['params'][number]
  value: number
  onChange: (v: number) => void
}) {
  const id = useId()
  if (spec.options.length > 0) {
    return (
      <div>
        <Label htmlFor={id}>{spec.label}</Label>
        <Select id={id} value={String(value)} onChange={(e) => onChange(Number(e.target.value))}>
          {spec.options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </Select>
        <p className="mt-1 text-xs text-muted">{spec.help}</p>
      </div>
    )
  }
  return (
    <div>
      <div className="mb-1 flex items-baseline justify-between gap-2">
        <label htmlFor={id} className="text-sm font-medium text-ink-2">{spec.label}</label>
        <span className="flex items-center gap-1">
          <input
            type="number"
            aria-label={`${spec.label} (${spec.unit})`}
            value={value}
            min={spec.min}
            max={spec.max}
            step={spec.step || 'any'}
            onChange={(e) => {
              if (e.target.value === '') return // campo apagado durante a digitação: não envia 0
              const v = Number(e.target.value)
              if (Number.isFinite(v)) onChange(v)
            }}
            className="w-20 rounded-md border border-line bg-white px-2 py-1 text-right text-sm tabular-nums"
          />
          <span className="text-xs text-muted">{spec.unit}</span>
        </span>
      </div>
      <input
        id={id}
        type="range"
        className="w-full"
        min={spec.min}
        max={spec.max}
        step={spec.step || 'any'}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
      />
      <p className="mt-0.5 text-xs text-muted">{spec.help}</p>
    </div>
  )
}

/** Medidas editáveis direto no editor, com carga/salvamento em perfil. */
export function MeasurementsPanel({
  item,
  draft,
  issues,
}: {
  item: CatalogItem
  draft: EditorDraft
  issues: Partial<Record<MeasurementName, MeasurementIssue>>
}) {
  const qc = useQueryClient()
  const guide = useQuery(guideQuery)
  const profiles = useQuery(profilesQuery)
  const sizes = useQuery(standardSizesQuery)
  const [showAll, setShowAll] = useState(false)
  const [newName, setNewName] = useState('')
  const [msg, setMsg] = useState<string | null>(null)
  const required = item.requiredMeasurements.map((r) => r.jsonName)

  const saveProfile = useMutation({
    mutationFn: (mode: 'update' | 'create') => {
      const body = { name: mode === 'create' ? newName.trim() : draft.profileName ?? 'Perfil', measurements: cleanMeasurements(draft.measurements) }
      if (mode === 'update' && draft.profileId) {
        const current = profiles.data?.find((p) => p.id === draft.profileId)
        return apiSend<Profile>('PUT', '/profiles/' + draft.profileId, {
          ...body,
          notes: current?.notes,
          // preserva medidas do perfil que não estão no rascunho
          measurements: { ...current?.measurements, ...body.measurements },
        })
      }
      return apiSend<Profile>('POST', '/profiles', body)
    },
    onSuccess: (p, mode) => {
      qc.invalidateQueries({ queryKey: ['profiles'] })
      editorStore.set({ profileId: p.id, profileName: p.name })
      setNewName('')
      setMsg(mode === 'create' ? `Perfil "${p.name}" criado.` : `Perfil "${p.name}" atualizado.`)
    },
  })

  const visible: MeasurementName[] = showAll ? MEASUREMENT_GROUPS.flatMap((g) => g.names) : required
  const dirty = (() => {
    if (!draft.profileId) return false
    const p = profiles.data?.find((x) => x.id === draft.profileId)
    if (!p) return false
    return required.some((k) => (p.measurements[k] ?? undefined) !== draft.measurements[k])
  })()

  return (
    <div className="space-y-4">
      <div className="grid gap-3">
        <div>
          <Label htmlFor="profile-select">Carregar perfil</Label>
          <Select
            id="profile-select"
            value={draft.profileId ?? ''}
            onChange={(e) => {
              const p = profiles.data?.find((x) => x.id === e.target.value)
              setMsg(null)
              if (p) editorStore.set({ measurements: { ...p.measurements }, profileId: p.id, profileName: p.name })
              else editorStore.set({ profileId: null, profileName: null })
            }}
          >
            <option value="">— medidas avulsas —</option>
            {profiles.data?.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </Select>
        </div>
        <div>
          <Label htmlFor="size-select">Ou partir de um tamanho padrão</Label>
          <Select
            id="size-select"
            value=""
            onChange={(e) => {
              const s = sizes.data?.find((x) => x.id === e.target.value)
              if (s) editorStore.set({ measurements: { ...s.measurements }, profileId: null, profileName: s.label })
            }}
          >
            <option value="">Escolher…</option>
            {sizes.data?.map((s) => <option key={s.id} value={s.id}>{s.label}</option>)}
          </Select>
        </div>
      </div>

      <div className="flex items-center justify-between">
        <p className="text-sm text-ink-2">{showAll ? 'Todas as medidas' : 'Medidas usadas neste modelo'}</p>
        <Button variant="ghost" className="px-2 py-1 text-xs" onClick={() => setShowAll((v) => !v)}>
          {showAll ? 'Só as necessárias' : 'Mostrar todas'}
        </Button>
      </div>

      <div className="grid gap-3">
        {guide.data &&
          visible.map((name) => {
            const g = guide.data.find((x) => x.jsonName === name)
            if (!g) return null
            return (
              <MeasurementInput
                key={name}
                compact
                guide={g}
                required={required.includes(name)}
                value={draft.measurements[name]}
                issue={issues[name]}
                onChange={(v) => { setMsg(null); editorStore.set((s) => ({ measurements: { ...s.measurements, [name]: v } })) }}
              />
            )
          })}
      </div>

      <div className="space-y-2 border-t border-line pt-4">
        {draft.profileId && (
          <Button
            variant={dirty ? 'primary' : 'secondary'}
            className="w-full"
            disabled={saveProfile.isPending || Object.keys(issues).length > 0}
            onClick={() => saveProfile.mutate('update')}
          >
            {dirty ? `Salvar alterações em "${draft.profileName}"` : `Salvar em "${draft.profileName}"`}
          </Button>
        )}
        <div className="flex gap-2">
          <TextInput aria-label="Nome do novo perfil" placeholder="Nome do novo perfil" value={newName} onChange={(e) => setNewName(e.target.value)} />
          <Button
            variant="secondary"
            disabled={!newName.trim() || saveProfile.isPending || Object.keys(issues).length > 0}
            onClick={() => saveProfile.mutate('create')}
          >
            Salvar
          </Button>
        </div>
        {msg && <Banner tone="ok">{msg}</Banner>}
        {saveProfile.isError && <Banner tone="error">{errorMessage(saveProfile.error)}</Banner>}
      </div>
    </div>
  )
}

export function SeamsPanel({ draft }: { draft: EditorDraft }) {
  const fields: { key: keyof EditorDraft['seams']; label: string; help: string; max: number }[] = [
    { key: 'seamCm', label: 'Margem de costura', help: 'Costuras laterais, ombro, cintura. Comum: 1 cm (overloque 0,7).', max: 5 },
    { key: 'hemCm', label: 'Margem de barra', help: 'Barra dobrada. Comum: 2–3 cm; godê: 1 cm.', max: 8 },
    { key: 'finishCm', label: 'Margem de acabamento', help: 'Decote com ribana, cava com viés. Comum: 0,7 cm.', max: 5 },
  ]
  return (
    <div className="space-y-4">
      {fields.map((f) => (
        <div key={f.key}>
          <Label htmlFor={`seam-${f.key}`}>{f.label} (cm)</Label>
          <TextInput
            id={`seam-${f.key}`}
            type="number"
            min={0}
            max={f.max}
            step={0.1}
            value={draft.seams[f.key]}
            onChange={(e) => {
              const v = Number(e.target.value)
              if (Number.isFinite(v) && v >= 0 && v <= f.max) editorStore.set((s) => ({ seams: { ...s.seams, [f.key]: v } }))
            }}
          />
          <p className="mt-1 text-xs text-muted">{f.help}</p>
        </div>
      ))}
      <div>
        <Label htmlFor="fabric-width">Largura do tecido (cm)</Label>
        <TextInput
          id="fabric-width"
          type="number"
          min={50}
          max={320}
          step={5}
          value={draft.fabricWidthCm}
          onChange={(e) => {
            const v = Number(e.target.value)
            if (Number.isFinite(v) && v >= 50 && v <= 320) editorStore.set({ fabricWidthCm: v })
          }}
        />
        <p className="mt-1 text-xs text-muted">Usada para estimar quantos metros comprar.</p>
      </div>
    </div>
  )
}
