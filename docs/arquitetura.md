# Arquitetura

```
frontend (React/Vite :5173)  ──/api (proxy)──▶  backend (Spring Boot :8080)
  features/                                       modules/
   measurements  ─────────────── /profiles ───▶    measurements  (perfis, guia, tamanhos padrão)  ─┐
   new-pattern   ── /ai/analyze-garment ──────▶    ai            (Claude visão → GarmentSuggestion)│
   editor        ── /patterns/draft|pdf ──────▶    pattern       (motor puro: geometria + drafters)  ├─ H2 (arquivo) + Flyway
   try-on        (3D local, sem backend)           export        (PDFBox: capa + folhas/plotter)     │
   projects      ── /projects ────────────────▶    projects      (projetos salvos)                  ─┘
  shared/ (api client, queries, editorStore, UI)   shared/ (erros, JSON, config)
```

## Decisões
| Decisão | Motivo |
|---|---|
| Java/Spring no backend | Usuário já tem Java 21; motor geométrico e PDF vetorial precisos e testáveis; SDK oficial da Anthropic em Java. |
| Motor de modelagem sem Spring | Determinístico, testável por tabela, reaproveitável (CLI, batch, outra UI). |
| Geometria como polilinhas densas (mm) | Simplifica margem de costura (offset por aresta), PDF e SVG; curvas são amostradas (Bézier/arcos/Catmull-Rom). |
| PDF gerado no servidor (PDFBox) | Escala exata em pontos, fontes padrão, nada depende do navegador. |
| H2 em arquivo (`backend/data/`) | Zero instalação. Modo PostgreSQL ligado para migrar depois (trocar URL + `CLOB`→`TEXT`). |
| Vite SPA (não Next.js) | App local sem SEO/SSR; 3D e canvas são client-side. |
| Provador 3D paramétrico no front | Resposta instantânea ao mexer parâmetros; sem custo de servidor. |
| IA só no backend | Chave nunca vai ao navegador; resposta validada contra o catálogo. |

## Motor de modelagem (backend `modules/pattern`)
- `geometry/` — `Vec2`, `Curves` (cubic, arc, smoothThrough, comprimento, ponto/tangente por distância), `Polygons`.
- `domain/` — `Edge` (trecho da linha de costura com tipo SEAM/HEM/FOLD/FINISH), `SeamAllowance` (gera linha de corte com margem por aresta e cantos por interseção), `PieceBuilder` (piques, fio, rótulo, validação de continuidade), `PatternEngine` (valida medidas, limita parâmetros, nomeia peças A, B, C..., estima tecido), `FabricEstimator`, `GarmentCatalog`.
- `drafting/` — um `GarmentDrafter` por modelo: `StraightSkirtDrafter`, `CircleSkirtDrafter`, `TShirtDrafter` (+ `Waistband`).

Fluxo: `DraftRequest` → `PatternEngine.draft` → `Pattern` (peças com seamLine/cutLine/foldLines/notches/grainline/internalLines em mm) → JSON (pontos `[x,y]`) ou `PdfPatternRenderer`.

## PDF (`modules/export`)
`SheetLayout` distribui peças numa folha virtual (prateleiras) e, para papel, testa nº de colunas minimizando páginas usadas; folhas sem peça são omitidas. `PdfPatternRenderer` desenha capa (instruções, margens, peças, tecido, avisos, medidas, mapa) e páginas com recorte (clip) da área útil, borda de colagem, losangos de alinhamento, quadrado de teste na 1ª folha.

## IA (`modules/ai`)
`ClaudeGarmentVision` monta um system prompt a partir do catálogo (chaves, unidades, limites), envia a imagem (base64) e pede `GarmentAnalysis` via saída estruturada (classe Java → JSON schema). `AiController` valida tipo de arquivo pelos bytes, limita tamanho, descarta chaves desconhecidas e aplica `ParamSpec.clamp`. Fallbacks do servidor habilitados (`server-side-fallback-2026-07-01`, `fallbacks: "default"`) para recusas.

## Frontend
- `shared/api` — `client.ts` (fetch + `ApiError`), `types.ts` (espelha o contrato), `queries.ts` (queryOptions).
- `shared/state/editorStore.ts` — rascunho do molde (modelo, parâmetros, medidas, margens, imagem, projeto) em `useSyncExternalStore` + sessionStorage.
- `features/editor` — prévia SVG (`PatternPreview`), painéis (parâmetros, medidas editáveis com salvar no perfil, margens), exportar PDF, salvar projeto.
- `features/try-on` — `bodyModel.ts` (puro: corpo e roupa em anéis elípticos) + `TryOnView.tsx` (react-three-fiber).
- `features/new-pattern` — prancheta (`SketchCanvas`), upload de foto (redimensiona p/ ≤1600 px), análise por IA, escolha manual.
