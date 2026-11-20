import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { lazy, Suspense, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiBlob, apiSend, ApiError, errorMessage } from '@/shared/api/client'
import { catalogQuery, guideQuery } from '@/shared/api/queries'
import type { CatalogItem, DraftRequest, PaperSize, Pattern, Project, ProjectRequest } from '@/shared/api/types'
import { downloadBlob } from '@/shared/lib/download'
import { cleanMeasurements, validateMeasurements } from '@/shared/lib/measurements'
import { useDebounced } from '@/shared/lib/useDebounced'
import { editorStore, useEditorDraft, type EditorDraft } from '@/shared/state/editorStore'
import { Badge, Banner, Button, Card, Label, Select, Spinner, TextInput } from '@/shared/ui/ui'
import { MeasurementsPanel, ParamsPanel, SeamsPanel } from './panels'
import { PatternPreview } from './PatternPreview'

const TryOnView = lazy(() => import('@/features/try-on/TryOnView'))

type SideTab = 'modelo' | 'medidas' | 'margens'
type MainTab = 'molde' | '3d' | 'referencia'

export default function EditorPage() {
  const draft = useEditorDraft()
  const catalog = useQuery(catalogQuery)
  const item = catalog.data?.find((c) => c.type === draft.garmentType)

  if (catalog.isPending) return <Spinner />
  if (catalog.isError) return <Banner tone="error">{errorMessage(catalog.error)}</Banner>
  if (!item) return <GarmentPicker items={catalog.data} />
  return <Editor item={item} draft={draft} />
}

function GarmentPicker({ items }: { items: CatalogItem[] }) {
  return (
    <div className="space-y-4">
      <h1 className="text-3xl font-semibold">Escolha o modelo</h1>
      <p className="text-ink-2">
        Ou <Link className="text-accent underline" to="/novo">comece por um desenho/foto</Link> e deixe a IA sugerir.
      </p>
      <div className="grid gap-4 md:grid-cols-3">
        {items.map((c) => (
          <Card key={c.type} className="flex flex-col p-5">
            <h2 className="text-xl font-semibold">{c.label}</h2>
            <p className="mt-1 flex-1 text-sm text-ink-2">{c.description}</p>
            <Button className="mt-4" onClick={() => editorStore.set({ garmentType: c.type, params: {} })}>Usar este modelo</Button>
          </Card>
        ))}
      </div>
    </div>
  )
}

function buildRequest(draft: EditorDraft): DraftRequest | null {
  if (!draft.garmentType) return null
  return {
    garmentType: draft.garmentType,
    measurements: cleanMeasurements(draft.measurements),
    params: draft.params,
    seams: draft.seams,
    fabricWidthCm: draft.fabricWidthCm,
  }
}

function Editor({ item, draft }: { item: CatalogItem; draft: EditorDraft }) {
  const guide = useQuery(guideQuery)
  const [side, setSide] = useState<SideTab>(draft.profileId || Object.keys(draft.measurements).length ? 'modelo' : 'medidas')
  const [main, setMain] = useState<MainTab>('molde')

  const required = item.requiredMeasurements.map((r) => r.jsonName)
  const issues = guide.data ? validateMeasurements(draft.measurements, required, guide.data) : {}
  const ready = guide.data !== undefined && Object.keys(issues).length === 0

  // debounce sobre a STRING (comparada por valor) — um objeto novo a cada render reiniciaria o timer para sempre
  const requestJson = useDebounced(JSON.stringify(buildRequest(draft)), 250)
  const request = useMemo(() => JSON.parse(requestJson) as DraftRequest | null, [requestJson])
  const pattern = useQuery({
    queryKey: ['draft', request],
    queryFn: ({ signal }) => apiSend<Pattern>('POST', '/patterns/draft', request, signal),
    enabled: ready && request !== null,
    placeholderData: keepPreviousData,
    retry: false,
  })

  const missingCount = Object.values(issues).filter((i) => i === 'missing').length

  return (
    <div className="grid gap-6 lg:grid-cols-[340px_1fr]">
      <aside aria-label="Configurações do molde" className="space-y-4">
        <Card className="p-4">
          <div className="mb-3 flex items-start justify-between gap-2">
            <div>
              <p className="text-xs uppercase tracking-wider text-muted">Modelo</p>
              <h1 className="text-xl font-semibold">{item.label}</h1>
            </div>
            <Button variant="ghost" className="px-2 py-1 text-xs" onClick={() => editorStore.set({ garmentType: null })}>Trocar</Button>
          </div>
          <div role="tablist" aria-label="Seções" className="mb-4 grid grid-cols-3 gap-1 rounded-lg bg-paper-2 p-1 text-sm">
            {(['modelo', 'medidas', 'margens'] as SideTab[]).map((t) => (
              <button
                key={t}
                role="tab"
                type="button"
                aria-selected={side === t}
                onClick={() => setSide(t)}
                className="rounded-md px-2 py-1.5 font-medium capitalize text-ink-2 aria-selected:bg-card aria-selected:text-ink aria-selected:shadow-sm"
              >
                {t === 'medidas' && missingCount > 0 ? `medidas (${missingCount})` : t}
              </button>
            ))}
          </div>
          <div role="tabpanel">
            {side === 'modelo' && <ParamsPanel item={item} draft={draft} />}
            {side === 'medidas' && <MeasurementsPanel item={item} draft={draft} issues={issues} />}
            {side === 'margens' && <SeamsPanel draft={draft} />}
          </div>
        </Card>
        {draft.aiNotes.length > 0 && (
          <Card className="p-4">
            <h2 className="mb-2 text-base font-semibold">Observações da leitura da imagem</h2>
            <ul className="list-disc space-y-1 pl-5 text-sm text-ink-2">
              {draft.aiNotes.map((n, i) => <li key={i}>{n}</li>)}
            </ul>
          </Card>
        )}
      </aside>

      <div className="min-w-0 space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          <div role="tablist" aria-label="Visualização" className="flex gap-1 rounded-lg bg-paper-2 p-1 text-sm">
            {([
              ['molde', 'Molde'],
              ['3d', 'Provador 3D'],
              ...(draft.sketchDataUrl ? [['referencia', 'Referência']] : []),
            ] as [MainTab, string][]).map(([t, label]) => (
              <button
                key={t}
                role="tab"
                type="button"
                aria-selected={main === t}
                onClick={() => setMain(t)}
                className="rounded-md px-3 py-1.5 font-medium text-ink-2 aria-selected:bg-card aria-selected:text-ink aria-selected:shadow-sm"
              >
                {label}
              </button>
            ))}
          </div>
          {pattern.isFetching && <Spinner label="Recalculando" />}
          {draft.profileName && <Badge>Medidas: {draft.profileName}</Badge>}
        </div>

        {!ready && guide.data && (
          <Banner tone="warn">
            {missingCount > 0
              ? `Faltam ${missingCount} medida(s) para este modelo. `
              : 'Há medidas fora do intervalo plausível. '}
            <button type="button" className="font-medium underline" onClick={() => setSide('medidas')}>Abrir medidas</button>
          </Banner>
        )}
        {pattern.isError && <DraftError error={pattern.error} />}

        <Card className="p-4">
          {main === 'molde' && (
            pattern.data ? <PatternPreview pattern={pattern.data} stale={pattern.isPlaceholderData || pattern.isFetching} /> : (
              <div className="grid h-80 place-items-center text-sm text-muted">
                {ready ? <Spinner label="Gerando molde" /> : 'Preencha as medidas para ver o molde.'}
              </div>
            )
          )}
          {main === '3d' && (
            <Suspense fallback={<Spinner label="Carregando provador" />}>
              <TryOnView garmentType={item.type} params={pattern.data?.params ?? draft.params} measurements={draft.measurements} />
            </Suspense>
          )}
          {main === 'referencia' && draft.sketchDataUrl && (
            <img src={draft.sketchDataUrl} alt="Desenho ou foto de referência da roupa" className="mx-auto max-h-[70vh] rounded-lg" />
          )}
        </Card>

        {pattern.data && <Summary pattern={pattern.data} />}

        <div className="grid gap-4 md:grid-cols-2">
          <ExportCard draft={draft} disabled={!pattern.data || !ready} label={item.label} />
          <SaveProjectCard draft={draft} disabled={!ready} />
        </div>
      </div>
    </div>
  )
}

function DraftError({ error }: { error: unknown }) {
  if (error instanceof ApiError && error.code === 'INVALID_MEASUREMENTS') {
    return <Banner tone="warn">{error.message}</Banner>
  }
  return <Banner tone="error">{errorMessage(error)}</Banner>
}

function Summary({ pattern }: { pattern: Pattern }) {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <Card className="p-4">
        <h2 className="mb-2 text-base font-semibold">Peças</h2>
        <ul className="space-y-1 text-sm">
          {pattern.pieces.map((p) => (
            <li key={p.id} className="flex justify-between gap-2">
              <span><strong className="font-display">{p.code}</strong> · {p.name}</span>
              <span className="text-muted">
                {p.cutQuantity}x{p.cutOnFold ? ' na dobra' : ''}{p.material !== 'Tecido principal' ? ` · ${p.material}` : ''}
              </span>
            </li>
          ))}
        </ul>
      </Card>
      <Card className="p-4">
        <h2 className="mb-2 text-base font-semibold">Tecido estimado</h2>
        <ul className="space-y-1 text-sm">
          {pattern.fabric.map((f) => (
            <li key={f.material}>
              <strong>{(f.lengthMm / 1000).toFixed(2).replace('.', ',')} m</strong> de {f.material.toLowerCase()} ({(f.fabricWidthMm / 1000).toFixed(2).replace('.', ',')} m de largura)
            </li>
          ))}
        </ul>
        {pattern.warnings.length > 0 && (
          <ul className="mt-3 space-y-1 text-sm text-ink-2">
            {pattern.warnings.map((w, i) => <li key={i} className="rounded bg-tape/15 px-2 py-1">⚠ {w}</li>)}
          </ul>
        )}
      </Card>
    </div>
  )
}

function ExportCard({ draft, disabled, label }: { draft: EditorDraft; disabled: boolean; label: string }) {
  const [paper, setPaper] = useState<PaperSize>('A4')
  const [title, setTitle] = useState('')
  const [showSeam, setShowSeam] = useState(true)
  const exportPdf = useMutation({
    mutationFn: async () => {
      const req = buildRequest(draft)
      if (!req) throw new Error('Escolha um modelo')
      const { blob, filename } = await apiBlob('/patterns/pdf', {
        draft: req,
        paper,
        title: title.trim() || draft.projectName || label,
        profileName: draft.profileName ?? undefined,
        showSeamLine: showSeam,
      })
      downloadBlob(blob, filename ?? 'molde.pdf')
    },
  })
  return (
    <Card className="space-y-3 p-4">
      <h2 className="text-base font-semibold">Baixar PDF para imprimir</h2>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <Label htmlFor="paper">Papel</Label>
          <Select id="paper" value={paper} onChange={(e) => setPaper(e.target.value as PaperSize)}>
            <option value="A4">A4 (impressora de casa)</option>
            <option value="LETTER">Carta</option>
            <option value="A3">A3</option>
            <option value="A0">A0 (gráfica)</option>
            <option value="PLOTTER_914">Plotter 91,4 cm</option>
            <option value="PLOTTER_1067">Plotter 106,7 cm</option>
          </Select>
        </div>
        <div>
          <Label htmlFor="pdf-title">Título</Label>
          <TextInput id="pdf-title" value={title} maxLength={80} placeholder={draft.projectName || label} onChange={(e) => setTitle(e.target.value)} />
        </div>
      </div>
      <label className="flex items-center gap-2 text-sm text-ink-2">
        <input type="checkbox" checked={showSeam} onChange={(e) => setShowSeam(e.target.checked)} /> Imprimir linha de costura tracejada
      </label>
      <Button className="w-full" disabled={disabled || exportPdf.isPending} onClick={() => exportPdf.mutate()}>
        {exportPdf.isPending ? 'Gerando PDF…' : 'Baixar PDF em tamanho real'}
      </Button>
      <p className="text-xs text-muted">Imprima a 100% (sem “ajustar à página”) e confira o quadrado de 5 cm antes de cortar.</p>
      {exportPdf.isError && <Banner tone="error">{errorMessage(exportPdf.error)}</Banner>}
    </Card>
  )
}

function SaveProjectCard({ draft, disabled }: { draft: EditorDraft; disabled: boolean }) {
  const qc = useQueryClient()
  const [ok, setOk] = useState(false)
  const save = useMutation({
    mutationFn: () => {
      if (!draft.garmentType) throw new Error('Escolha um modelo')
      const body: ProjectRequest = {
        name: draft.projectName.trim() || 'Molde sem nome',
        garmentType: draft.garmentType,
        profileId: draft.profileId ?? undefined,
        measurements: cleanMeasurements(draft.measurements),
        params: draft.params,
        seams: draft.seams,
        sketchDataUrl: draft.sketchDataUrl ?? undefined,
      }
      return draft.projectId
        ? apiSend<Project>('PUT', '/projects/' + draft.projectId, body)
        : apiSend<Project>('POST', '/projects', body)
    },
    onSuccess: (p) => {
      editorStore.set({ projectId: p.id, projectName: p.name })
      qc.invalidateQueries({ queryKey: ['projects'] })
      setOk(true)
    },
  })
  return (
    <Card className="space-y-3 p-4">
      <h2 className="text-base font-semibold">Salvar projeto</h2>
      <div>
        <Label htmlFor="project-name">Nome do projeto</Label>
        <TextInput
          id="project-name"
          value={draft.projectName}
          maxLength={120}
          placeholder="Ex.: Saia midi de linho"
          onChange={(e) => { setOk(false); editorStore.set({ projectName: e.target.value }) }}
        />
      </div>
      <div className="flex gap-2">
        <Button className="flex-1" variant="secondary" disabled={disabled || save.isPending} onClick={() => { setOk(false); save.mutate() }}>
          {save.isPending ? 'Salvando…' : draft.projectId ? 'Salvar alterações' : 'Salvar projeto'}
        </Button>
        {draft.projectId && (
          <Button variant="ghost" onClick={() => editorStore.set({ projectId: null, projectName: draft.projectName + ' (cópia)' })}>
            Duplicar
          </Button>
        )}
      </div>
      {ok && <Banner tone="ok">Projeto salvo.</Banner>}
      {save.isError && <Banner tone="error">{errorMessage(save.error)}</Banner>}
    </Card>
  )
}
