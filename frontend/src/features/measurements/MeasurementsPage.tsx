import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiSend, errorMessage } from '@/shared/api/client'
import { guideQuery, profilesQuery, standardSizesQuery } from '@/shared/api/queries'
import type { Measurements, Profile } from '@/shared/api/types'
import { cleanMeasurements, MEASUREMENT_GROUPS, validateMeasurements } from '@/shared/lib/measurements'
import { editorStore } from '@/shared/state/editorStore'
import { MeasurementInput } from '@/shared/ui/MeasurementInput'
import { Banner, Button, Card, Label, Select, Spinner, TextInput } from '@/shared/ui/ui'

interface FormState {
  id: string | null
  name: string
  notes: string
  measurements: Measurements
}

const empty: FormState = { id: null, name: '', notes: '', measurements: {} }

export default function MeasurementsPage() {
  const profiles = useQuery(profilesQuery)
  const [form, setForm] = useState<FormState>(empty)
  const [savedMsg, setSavedMsg] = useState<string | null>(null)

  const edit = (p: Profile) => {
    setForm({ id: p.id, name: p.name, notes: p.notes ?? '', measurements: p.measurements })
    setSavedMsg(null)
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
      <aside aria-label="Perfis de medidas">
        <div className="mb-3 flex items-center justify-between">
          <h1 className="text-2xl font-semibold">Medidas</h1>
          <Button variant="secondary" onClick={() => { setForm(empty); setSavedMsg(null) }}>+ Novo</Button>
        </div>
        {profiles.isPending && <Spinner />}
        {profiles.isError && <Banner tone="error">{errorMessage(profiles.error)}</Banner>}
        {profiles.data?.length === 0 && (
          <p className="text-sm text-muted">Nenhum perfil ainda. Crie o primeiro ao lado — dá para partir de um tamanho padrão.</p>
        )}
        <ul className="space-y-2">
          {profiles.data?.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                onClick={() => edit(p)}
                aria-current={form.id === p.id ? 'true' : undefined}
                className="w-full rounded-lg border border-line bg-card px-3 py-2 text-left hover:border-ink-2 aria-[current=true]:border-ink aria-[current=true]:bg-paper-2"
              >
                <span className="block font-medium">{p.name}</span>
                <span className="text-xs text-muted">
                  Busto {p.measurements.bust ?? '–'} · Cintura {p.measurements.waist ?? '–'} · Quadril {p.measurements.hip ?? '–'}
                </span>
              </button>
            </li>
          ))}
        </ul>
      </aside>

      <ProfileEditor
        key={form.id ?? 'novo'}
        initial={form}
        onSaved={(p, msg) => { edit(p); setSavedMsg(msg) }}
        onDeleted={() => { setForm(empty); setSavedMsg('Perfil excluído.') }}
        savedMsg={savedMsg}
      />
    </div>
  )
}

function ProfileEditor({
  initial,
  onSaved,
  onDeleted,
  savedMsg,
}: {
  initial: FormState
  onSaved: (p: Profile, msg: string) => void
  onDeleted: () => void
  savedMsg: string | null
}) {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const guide = useQuery(guideQuery)
  const sizes = useQuery(standardSizesQuery)
  const [form, setForm] = useState<FormState>(initial)
  const [touched, setTouched] = useState(false)

  const issues = guide.data ? validateMeasurements(form.measurements, [], guide.data) : {}
  const hasIssues = Object.keys(issues).length > 0
  const nameError = touched && !form.name.trim() ? 'Dê um nome ao perfil' : null

  const save = useMutation({
    mutationFn: () => {
      const body = { name: form.name.trim(), notes: form.notes || undefined, measurements: cleanMeasurements(form.measurements) }
      return form.id ? apiSend<Profile>('PUT', '/profiles/' + form.id, body) : apiSend<Profile>('POST', '/profiles', body)
    },
    onSuccess: (p) => {
      qc.invalidateQueries({ queryKey: ['profiles'] })
      onSaved(p, form.id ? 'Medidas atualizadas.' : 'Perfil criado.')
    },
  })

  const remove = useMutation({
    mutationFn: () => apiSend<void>('DELETE', '/profiles/' + form.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['profiles'] })
      onDeleted()
    },
  })

  const setValue = (name: keyof Measurements, v: number | undefined) =>
    setForm((f) => ({ ...f, measurements: { ...f.measurements, [name]: v } }))

  const useInEditor = () => {
    editorStore.set({ measurements: cleanMeasurements(form.measurements), profileId: form.id, profileName: form.name || null })
    navigate('/editor')
  }

  return (
    <Card className="p-5">
      <form
        onSubmit={(e) => {
          e.preventDefault()
          setTouched(true)
          if (!form.name.trim() || hasIssues) return
          save.mutate()
        }}
        noValidate
        className="space-y-6"
      >
        <div className="flex flex-wrap items-end gap-4">
          <div className="min-w-56 flex-1">
            <Label htmlFor="profile-name">Nome do perfil</Label>
            <TextInput
              id="profile-name"
              value={form.name}
              placeholder="Ex.: Ana, Cliente Maria, Eu"
              aria-invalid={nameError ? true : undefined}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
            {nameError && <p className="mt-1 text-xs text-danger">{nameError}</p>}
          </div>
          <div className="min-w-56">
            <Label htmlFor="std-size">Preencher com tamanho padrão</Label>
            <Select
              id="std-size"
              value=""
              onChange={(e) => {
                const s = sizes.data?.find((x) => x.id === e.target.value)
                if (s) setForm((f) => ({ ...f, measurements: { ...s.measurements }, name: f.name || s.label }))
              }}
            >
              <option value="">Escolher…</option>
              {['Feminino', 'Masculino'].map((g) => (
                <optgroup key={g} label={g}>
                  {sizes.data?.filter((s) => s.group === g).map((s) => <option key={s.id} value={s.id}>{s.label}</option>)}
                </optgroup>
              ))}
            </Select>
          </div>
        </div>

        <Banner tone="info">
          Meça com fita métrica sobre roupa fina, sem apertar. Todas as medidas podem ser editadas a qualquer momento — também
          direto no editor do molde.
        </Banner>

        {guide.isPending && <Spinner />}
        {guide.data &&
          MEASUREMENT_GROUPS.map((group) => (
            <fieldset key={group.title}>
              <legend className="mb-3 font-display text-lg font-semibold">{group.title}</legend>
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {group.names.map((name) => {
                  const g = guide.data.find((x) => x.jsonName === name)
                  if (!g) return null
                  return (
                    <MeasurementInput
                      key={name}
                      guide={g}
                      value={form.measurements[name]}
                      issue={issues[name]}
                      onChange={(v) => setValue(name, v)}
                    />
                  )
                })}
              </div>
            </fieldset>
          ))}

        <div>
          <Label htmlFor="profile-notes">Observações</Label>
          <TextInput id="profile-notes" value={form.notes} maxLength={2000} onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))} />
        </div>

        {save.isError && <Banner tone="error">{errorMessage(save.error)}</Banner>}
        {remove.isError && <Banner tone="error">{errorMessage(remove.error)}</Banner>}
        {savedMsg && !save.isPending && <Banner tone="ok">{savedMsg}</Banner>}
        {touched && hasIssues && <Banner tone="warn">Corrija as medidas marcadas em vermelho antes de salvar.</Banner>}

        <div className="flex flex-wrap gap-3">
          <Button type="submit" disabled={save.isPending}>{save.isPending ? 'Salvando…' : form.id ? 'Salvar alterações' : 'Criar perfil'}</Button>
          <Button variant="secondary" onClick={useInEditor} disabled={hasIssues}>Usar no editor de molde</Button>
          {form.id && (
            <Button
              variant="danger"
              className="ml-auto"
              disabled={remove.isPending}
              onClick={() => { if (window.confirm(`Excluir o perfil "${form.name}"?`)) remove.mutate() }}
            >
              Excluir
            </Button>
          )}
        </div>
      </form>
    </Card>
  )
}
