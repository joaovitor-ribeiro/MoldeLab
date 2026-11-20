import { useMutation, useQuery } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiUpload, errorMessage } from '@/shared/api/client'
import { aiStatusQuery, catalogQuery } from '@/shared/api/queries'
import type { CatalogItem, GarmentSuggestion, GarmentType } from '@/shared/api/types'
import { dataUrlToBlob, readFileAsDataUrl, shrinkImage } from '@/shared/lib/download'
import { editorStore } from '@/shared/state/editorStore'
import { Badge, Banner, Button, Card, Label, Spinner, TextInput } from '@/shared/ui/ui'
import { SketchCanvas, type SketchHandle } from './SketchCanvas'

type Source = 'desenho' | 'foto' | 'catalogo'

export default function NewPatternPage() {
  const [source, setSource] = useState<Source>('desenho')
  const ai = useQuery(aiStatusQuery)
  const catalog = useQuery(catalogQuery)
  const navigate = useNavigate()
  const sketchRef = useRef<SketchHandle>(null)
  const [photo, setPhoto] = useState<string | null>(null)
  const [hint, setHint] = useState('')
  const [image, setImage] = useState<string | null>(null)

  const analyze = useMutation({
    mutationFn: async (dataUrl: string) => {
      const form = new FormData()
      const blob = dataUrlToBlob(dataUrl)
      form.append('image', blob, blob.type === 'image/png' ? 'roupa.png' : 'roupa.jpg')
      if (hint.trim()) form.append('hint', hint.trim())
      return apiUpload<GarmentSuggestion>('/ai/analyze-garment', form)
    },
  })

  const currentImage = (): string | null => (source === 'desenho' ? sketchRef.current?.toDataUrl() ?? null : photo)

  const startAnalysis = () => {
    const img = currentImage()
    if (!img) return
    setImage(img)
    analyze.mutate(img)
  }

  const goToEditor = (type: GarmentType, params: Record<string, number>, notes: string[] = [], keepImage = true) => {
    editorStore.set({
      garmentType: type,
      params,
      aiNotes: notes,
      sketchDataUrl: keepImage ? image ?? currentImage() : null,
      projectId: null,
    })
    navigate('/editor')
  }

  const aiEnabled = ai.data?.enabled === true

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold">Novo molde</h1>
        <p className="text-ink-2">Desenhe a roupa, envie uma foto ou escolha direto um modelo base.</p>
      </div>

      <div role="tablist" aria-label="Como começar" className="flex flex-wrap gap-2">
        {([
          ['desenho', 'Desenhar'],
          ['foto', 'Enviar foto'],
          ['catalogo', 'Escolher modelo'],
        ] as [Source, string][]).map(([s, label]) => (
          <button
            key={s}
            type="button"
            role="tab"
            aria-selected={source === s}
            onClick={() => { setSource(s); analyze.reset() }}
            className="rounded-full border border-line bg-card px-4 py-2 text-sm font-medium aria-selected:border-ink aria-selected:bg-ink aria-selected:text-paper"
          >
            {label}
          </button>
        ))}
      </div>

      {source !== 'catalogo' && ai.data && !aiEnabled && (
        <Banner tone="warn">
          A leitura automática por IA está desligada (falta configurar <code>ANTHROPIC_API_KEY</code> no backend). Você ainda
          pode desenhar/enviar a imagem como referência e escolher o modelo manualmente abaixo.
        </Banner>
      )}

      {source === 'catalogo' ? (
        <CatalogGrid items={catalog.data} onPick={(t) => goToEditor(t, {}, [], false)} />
      ) : (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_380px]">
          <Card className="p-4">
            {source === 'desenho' ? (
              <SketchCanvas ref={sketchRef} />
            ) : (
              <div className="space-y-3">
                <Label htmlFor="garment-photo">Foto da roupa (PNG, JPEG ou WEBP, até 8 MB)</Label>
                <input
                  id="garment-photo"
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  className="block w-full text-sm"
                  onChange={async (e) => {
                    const f = e.target.files?.[0]
                    if (!f) return
                    setPhoto(await shrinkImage(await readFileAsDataUrl(f)))
                    analyze.reset()
                  }}
                />
                {photo ? (
                  <img src={photo} alt="Foto enviada da roupa" className="max-h-[60vh] rounded-lg border border-line object-contain" />
                ) : (
                  <div className="grid h-64 place-items-center rounded-lg border border-dashed border-line text-sm text-muted">
                    Nenhuma foto escolhida
                  </div>
                )}
              </div>
            )}
          </Card>

          <div className="space-y-4">
            <Card className="space-y-3 p-4">
              <h2 className="text-lg font-semibold">Ler a imagem</h2>
              <div>
                <Label htmlFor="hint">Dica para a leitura (opcional)</Label>
                <TextInput id="hint" maxLength={500} value={hint} placeholder="Ex.: saia midi rodada, de linho" onChange={(e) => setHint(e.target.value)} />
              </div>
              <Button className="w-full" disabled={!aiEnabled || analyze.isPending} onClick={startAnalysis}>
                {analyze.isPending ? 'Analisando…' : 'Analisar com IA'}
              </Button>
              {analyze.isPending && <Spinner label="Lendo silhueta, comprimento e detalhes" />}
              {analyze.isError && <Banner tone="error">{errorMessage(analyze.error)}</Banner>}
            </Card>

            {analyze.data && <SuggestionCard s={analyze.data} items={catalog.data} onUse={goToEditor} />}

            <Card className="space-y-2 p-4">
              <h2 className="text-base font-semibold">Ou escolha o modelo você mesmo(a)</h2>
              <p className="text-xs text-muted">A imagem fica salva como referência no editor.</p>
              <div className="grid gap-2">
                {catalog.data?.map((c) => (
                  <Button key={c.type} variant="secondary" className="justify-start" onClick={() => goToEditor(c.type, {}, [])}>
                    {c.label}
                  </Button>
                ))}
              </div>
            </Card>
          </div>
        </div>
      )}
    </div>
  )
}

function SuggestionCard({
  s,
  items,
  onUse,
}: {
  s: GarmentSuggestion
  items?: CatalogItem[]
  onUse: (t: GarmentType, p: Record<string, number>, notes: string[]) => void
}) {
  const item = items?.find((i) => i.type === s.garmentType)
  return (
    <Card className="space-y-3 border-accent/40 p-4">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-lg font-semibold">{s.supported && item ? item.label : 'Modelo ainda não suportado'}</h2>
        <Badge>confiança {Math.round(s.confidence * 100)}%</Badge>
      </div>
      {s.description && <p className="text-sm text-ink-2">{s.description}</p>}
      {item && Object.keys(s.params).length > 0 && (
        <dl className="grid grid-cols-2 gap-x-3 gap-y-1 text-sm">
          {Object.entries(s.params).map(([k, v]) => {
            const spec = item.params.find((p) => p.key === k)
            return (
              <div key={k} className="contents">
                <dt className="text-muted">{spec?.label ?? k}</dt>
                <dd className="text-right tabular-nums">
                  {spec?.options.length ? spec.options.find((o) => o.value === v)?.label ?? v : `${v} ${spec?.unit ?? ''}`}
                </dd>
              </div>
            )
          })}
        </dl>
      )}
      {s.notes.length > 0 && (
        <ul className="list-disc space-y-1 pl-5 text-sm text-ink-2">
          {s.notes.map((n, i) => <li key={i}>{n}</li>)}
        </ul>
      )}
      {s.supported && s.garmentType ? (
        <Button className="w-full" onClick={() => onUse(s.garmentType!, s.params, s.notes)}>Abrir no editor</Button>
      ) : (
        <p className="text-sm text-muted">Escolha o modelo mais próximo abaixo e ajuste os parâmetros.</p>
      )}
    </Card>
  )
}

function CatalogGrid({ items, onPick }: { items?: CatalogItem[]; onPick: (t: GarmentType) => void }) {
  if (!items) return <Spinner />
  return (
    <div className="grid gap-4 md:grid-cols-3">
      {items.map((c) => (
        <Card key={c.type} className="flex flex-col p-5">
          <h2 className="text-xl font-semibold">{c.label}</h2>
          <p className="mt-1 flex-1 text-sm text-ink-2">{c.description}</p>
          <p className="mt-3 text-xs text-muted">Medidas: {c.requiredMeasurements.map((r) => r.label.toLowerCase()).join(', ')}</p>
          <Button className="mt-4" onClick={() => onPick(c.type)}>Usar este modelo</Button>
        </Card>
      ))}
    </div>
  )
}
