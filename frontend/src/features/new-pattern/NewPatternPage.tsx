import { useMutation, useQuery } from '@tanstack/react-query'
import { useRef, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiUpload, errorMessage } from '@/shared/api/client'
import { aiStatusQuery, catalogQuery } from '@/shared/api/queries'
import type { CatalogItem, GarmentSuggestion, GarmentType } from '@/shared/api/types'
import { dataUrlToBlob, readFileAsDataUrl, shrinkImage } from '@/shared/lib/download'
import { editorStore } from '@/shared/state/editorStore'
import { Badge, Banner, Button, Card, Label, Spinner, TextInput } from '@/shared/ui/ui'
import { SketchCanvas, type SketchHandle } from './SketchCanvas'

type Source = 'foto' | 'desenho' | 'nenhuma'

const SOURCES: [Source, string][] = [
  ['foto', 'Enviar foto'],
  ['desenho', 'Desenhar'],
  ['nenhuma', 'Sem imagem'],
]

const MAX_BYTES = 25 * 1024 * 1024 // antes de reduzir; shrinkImage entrega ~1600 px em JPEG
const ACCEPTED = ['image/png', 'image/jpeg', 'image/webp']

/**
 * Fluxo em 3 passos: (1) imagem de referência — opcional; (2) modelo base — sugerido pela IA (opcional)
 * ou escolhido à mão; (3) editor: medidas, ajustes, 3D e PDF.
 */
export default function NewPatternPage() {
  const [source, setSource] = useState<Source>('foto')
  const ai = useQuery(aiStatusQuery)
  const catalog = useQuery(catalogQuery)
  const navigate = useNavigate()
  const sketchRef = useRef<SketchHandle>(null)
  const [photo, setPhoto] = useState<string | null>(null)
  const [sketchEmpty, setSketchEmpty] = useState(true)
  const [hint, setHint] = useState('')
  const [analyzedImage, setAnalyzedImage] = useState<string | null>(null)

  const analyze = useMutation({
    mutationFn: async (dataUrl: string) => {
      const form = new FormData()
      const blob = dataUrlToBlob(dataUrl)
      form.append('image', blob, blob.type === 'image/png' ? 'roupa.png' : 'roupa.jpg')
      if (hint.trim()) form.append('hint', hint.trim())
      return apiUpload<GarmentSuggestion>('/ai/analyze-garment', form)
    },
  })

  const hasImage = source === 'foto' ? photo !== null : source === 'desenho' ? !sketchEmpty : false
  const currentImage = (): string | null =>
    source === 'desenho' ? sketchRef.current?.toDataUrl() ?? null : source === 'foto' ? photo : null

  const changeSource = (s: Source) => {
    setSource(s)
    analyze.reset()
  }

  const startAnalysis = () => {
    const img = currentImage()
    if (!img) return
    setAnalyzedImage(img)
    analyze.mutate(img)
  }

  const goToEditor = (type: GarmentType, params: Record<string, number> = {}, notes: string[] = []) => {
    editorStore.set({
      garmentType: type,
      params,
      aiNotes: notes,
      sketchDataUrl: analyze.data ? analyzedImage : currentImage(),
      projectId: null,
    })
    navigate('/editor')
  }

  const aiEnabled = ai.data?.enabled === true

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold">Novo molde</h1>
        <p className="max-w-3xl text-ink-2">
          O molde é gerado a partir de um <strong>modelo base</strong> (saia, camiseta…) com as <strong>suas medidas</strong>.
          A imagem serve de referência e, se quiser, para a IA sugerir o modelo e os ajustes.
        </p>
      </div>

      <ol className="flex flex-wrap items-center gap-x-3 gap-y-2 text-sm" aria-label="Etapas">
        <StepPill n={1} active>Imagem da roupa <span className="text-muted">(opcional)</span></StepPill>
        <span aria-hidden className="text-muted">→</span>
        <StepPill n={2} active>Escolher o modelo</StepPill>
        <span aria-hidden className="text-muted">→</span>
        <StepPill n={3}>No editor: medidas, ajustes, 3D e PDF</StepPill>
      </ol>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_400px]">
        {/* PASSO 1 */}
        <Card className="space-y-4 self-start p-4 sm:p-5">
          <StepTitle n={1} title="Imagem da roupa" hint="Opcional. Fica salva no projeto como referência." />

          <div role="tablist" aria-label="Tipo de imagem" className="inline-flex flex-wrap gap-1 rounded-full border border-line bg-paper p-1">
            {SOURCES.map(([s, label]) => (
              <button
                key={s}
                type="button"
                role="tab"
                aria-selected={source === s}
                onClick={() => changeSource(s)}
                className="rounded-full px-4 py-1.5 text-sm font-medium text-ink-2 aria-selected:bg-ink aria-selected:text-paper"
              >
                {label}
              </button>
            ))}
          </div>

          {source === 'foto' && (
            <PhotoPicker
              photo={photo}
              onPhoto={(p) => {
                setPhoto(p)
                analyze.reset()
              }}
            />
          )}
          {source === 'desenho' && <SketchCanvas ref={sketchRef} onEmptyChange={setSketchEmpty} />}
          {source === 'nenhuma' && (
            <div className="grid h-48 place-items-center rounded-lg border border-dashed border-line px-6 text-center text-sm text-muted">
              Tudo bem — escolha o modelo no passo 2 e siga direto para o editor.
            </div>
          )}
        </Card>

        {/* PASSO 2 */}
        <Card className="space-y-5 self-start p-4 sm:p-5">
          <StepTitle n={2} title="Escolha o modelo" hint="Use a sugestão da IA ou escolha você mesmo(a) — os dois levam ao editor." />

          {source !== 'nenhuma' && (
            <section aria-labelledby="ai-title" className="space-y-3 rounded-lg border border-line bg-paper p-3">
              <div className="flex items-center justify-between gap-2">
                <h3 id="ai-title" className="font-semibold">Sugestão da IA</h3>
                <Badge>opcional</Badge>
              </div>
              <p className="text-xs text-muted">
                A IA olha a imagem e sugere o modelo e os ajustes (comprimento, rodado, manga…). Você pode mudar tudo no editor.
              </p>

              {ai.data && !aiEnabled ? (
                <p className="text-sm text-ink-2">A IA não está configurada neste servidor. Escolha o modelo logo abaixo.</p>
              ) : (
                <>
                  <div>
                    <Label htmlFor="hint">Dica para a IA (opcional)</Label>
                    <TextInput
                      id="hint"
                      maxLength={500}
                      value={hint}
                      placeholder="Ex.: saia midi rodada, de linho"
                      onChange={(e) => setHint(e.target.value)}
                    />
                  </div>
                  <Button className="w-full" disabled={!aiEnabled || !hasImage || analyze.isPending} onClick={startAnalysis}>
                    {analyze.isPending ? 'Analisando…' : 'Analisar imagem com IA'}
                  </Button>
                  {!hasImage && (
                    <p className="text-xs text-muted">
                      {source === 'foto' ? 'Envie uma foto no passo 1 para usar a IA.' : 'Desenhe a roupa no passo 1 para usar a IA.'}
                    </p>
                  )}
                  {analyze.isPending && <Spinner label="Lendo silhueta, comprimento e detalhes (até ~30 s)" />}
                  {analyze.isError && <Banner tone="error">{errorMessage(analyze.error)}</Banner>}
                </>
              )}

              {analyze.data && <SuggestionCard s={analyze.data} items={catalog.data} onUse={goToEditor} />}
            </section>
          )}

          <section aria-labelledby="manual-title" className="space-y-2">
            <h3 id="manual-title" className="font-semibold">
              {source === 'nenhuma' ? 'Qual modelo você quer?' : 'Ou escolha você mesmo(a)'}
            </h3>
            <p className="text-xs text-muted">Clique no modelo para abrir o editor.</p>
            {catalog.isPending && <Spinner />}
            {catalog.isError && <Banner tone="error">{errorMessage(catalog.error)}</Banner>}
            <div className="grid gap-2">
              {catalog.data?.map((c) => (
                <ModelOption key={c.type} item={c} onPick={() => goToEditor(c.type)} />
              ))}
            </div>
          </section>

          <p className="border-t border-line pt-3 text-xs text-muted">
            <strong className="text-ink-2">Depois:</strong> no editor você confere e edita suas medidas, ajusta o modelo,
            vê em 3D e baixa o PDF para imprimir.
          </p>
        </Card>
      </div>
    </div>
  )
}

function StepPill({ n, active, children }: { n: number; active?: boolean; children: ReactNode }) {
  return (
    <li className="flex items-center gap-2">
      <span
        className={`grid size-6 place-items-center rounded-full text-xs font-semibold ${active ? 'bg-ink text-paper' : 'border border-line text-muted'}`}
      >
        {n}
      </span>
      <span className={active ? 'text-ink' : 'text-muted'}>{children}</span>
    </li>
  )
}

function StepTitle({ n, title, hint }: { n: number; title: string; hint: string }) {
  return (
    <div className="flex gap-3">
      <span className="grid size-8 shrink-0 place-items-center rounded-full bg-ink text-sm font-semibold text-paper">{n}</span>
      <div>
        <h2 className="text-lg font-semibold leading-8">{title}</h2>
        <p className="text-sm text-muted">{hint}</p>
      </div>
    </div>
  )
}

/** Área grande clicável/arrastável para a foto, com prévia e ações claras depois de escolher. */
function PhotoPicker({ photo, onPhoto }: { photo: string | null; onPhoto: (p: string | null) => void }) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragOver, setDragOver] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const accept = async (f: File | undefined) => {
    if (!f) return
    if (!ACCEPTED.includes(f.type)) return setError('Formato não aceito. Use PNG, JPEG ou WEBP.')
    if (f.size > MAX_BYTES) return setError('Arquivo grande demais (máx. 25 MB).')
    setError(null)
    setLoading(true)
    try {
      onPhoto(await shrinkImage(await readFileAsDataUrl(f)))
    } catch {
      setError('Não foi possível ler esta imagem.')
    } finally {
      setLoading(false)
    }
  }

  const input = (
    <input
      ref={inputRef}
      id="garment-photo"
      type="file"
      accept={ACCEPTED.join(',')}
      className="sr-only"
      onChange={(e) => {
        void accept(e.target.files?.[0])
        e.target.value = ''
      }}
    />
  )

  if (photo) {
    return (
      <div className="space-y-3">
        {input}
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex-1">
            <Banner tone="ok">Foto carregada ✓ Agora escolha o modelo no passo 2.</Banner>
          </div>
          <Button variant="secondary" onClick={() => inputRef.current?.click()}>Trocar foto</Button>
          <Button variant="ghost" onClick={() => onPhoto(null)}>Remover</Button>
        </div>
        <img src={photo} alt="Foto enviada da roupa" className="max-h-[50vh] w-full rounded-lg border border-line bg-white object-contain" />
      </div>
    )
  }

  return (
    <div className="space-y-2">
      {input}
      <label
        htmlFor="garment-photo"
        onDragOver={(e) => {
          e.preventDefault()
          setDragOver(true)
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragOver(false)
          void accept(e.dataTransfer.files?.[0])
        }}
        className={`flex h-72 cursor-pointer flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed px-6 text-center transition-colors ${
          dragOver ? 'border-accent bg-accent/5' : 'border-line hover:border-ink-2 hover:bg-paper'
        }`}
      >
        <svg aria-hidden viewBox="0 0 24 24" className="size-10 text-muted" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M4 16.5V19a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-2.5M12 4v11m0-11-4 4m4-4 4 4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        {loading ? (
          <Spinner label="Preparando a imagem…" />
        ) : (
          <>
            <span className="inline-flex rounded-lg bg-ink px-4 py-2 text-sm font-medium text-paper">Escolher foto</span>
            <span className="text-sm text-ink-2">ou arraste a imagem para cá</span>
            <span className="text-xs text-muted">PNG, JPEG ou WEBP · de frente, com a peça inteira visível</span>
          </>
        )}
      </label>
      {error && <Banner tone="error">{error}</Banner>}
    </div>
  )
}

function ModelOption({ item, onPick }: { item: CatalogItem; onPick: () => void }) {
  return (
    <button
      type="button"
      onClick={onPick}
      className="group flex w-full items-center justify-between gap-3 rounded-lg border border-line bg-card px-3 py-2.5 text-left transition-colors hover:border-ink-2"
    >
      <span>
        <span className="block text-sm font-medium text-ink">{item.label}</span>
        <span className="line-clamp-2 block text-xs text-muted">{item.description}</span>
      </span>
      <span aria-hidden className="text-muted transition-transform group-hover:translate-x-0.5 group-hover:text-ink">→</span>
    </button>
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
    <div className="space-y-3 rounded-lg border border-accent/50 bg-card p-3">
      <div className="flex items-center justify-between gap-2">
        <h4 className="font-semibold">{s.supported && item ? `Sugestão: ${item.label}` : 'Modelo ainda não suportado'}</h4>
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
        <Button className="w-full" onClick={() => onUse(s.garmentType!, s.params, s.notes)}>
          Usar sugestão e abrir o editor →
        </Button>
      ) : (
        <p className="text-sm text-muted">Escolha o modelo mais próximo logo abaixo e ajuste no editor.</p>
      )}
    </div>
  )
}
