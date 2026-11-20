# MoldeLab — do desenho ao molde

App para quem costura: a pessoa **desenha ou fotografa uma roupa**, informa (e edita) suas **medidas**, ajusta o modelo, vê num **provador 3D** e baixa um **PDF em escala real** (folhas A4 lado a lado ou plotter) para imprimir, montar, cortar e costurar.

Leia antes de mudar algo: `docs/visao.md` (produto e escopo), `docs/arquitetura.md` (como as peças se encaixam), `docs/api-contracts.md` (contrato HTTP), `docs/modelagem/regras.md` (regras de modelagem RN-*), `docs/roadmap.md`.

## Stack

| Camada | Tecnologia | Pasta |
|---|---|---|
| Backend | Java 21, Spring Boot 3.5, Maven (`mvnw`), H2 em arquivo + Flyway, PDFBox 3, SDK oficial `anthropic-java` | `backend/` |
| Frontend | React 19 + Vite 8 + TypeScript 7, Tailwind v4, TanStack Query 5, react-three-fiber/drei (3D), Vitest | `frontend/` |
| IA | Claude (`claude-opus-5-5`) com visão + saída estruturada, só no backend | `backend/.../modules/ai` |

Pacote raiz Java: `com.moldelab`. Módulos em `com.moldelab.modules.<modulo>`: `measurements`, `pattern` (geometria + modelagem), `export` (PDF), `ai`, `projects`. Código compartilhado em `com.moldelab.shared`.

## Comandos

```bash
# tudo de uma vez (Windows): sobe backend (8080) e frontend (5173)
./start.ps1                     # ou start.cmd

# backend
cd backend && ./mvnw spring-boot:run      # API em http://localhost:8080
cd backend && ./mvnw verify               # testes (JUnit 5, MockMvc, H2 em memória)

# frontend
cd frontend && npm run dev                # http://localhost:5173 (proxy /api → 8080)
cd frontend && npm test                   # Vitest
cd frontend && npm run typecheck && npm run lint && npm run build
```

IA de leitura de imagem: defina `ANTHROPIC_API_KEY` no ambiente do backend.

Banco: sem `DATABASE_URL` usa H2 em arquivo; com ela, PostgreSQL (produção no Neon + Cloud Run, ver `docs/deploy.md`). Imagem única em `Dockerfile` (backend serve o front). Sem a chave, o app funciona (escolha manual do modelo) e `/api/ai/status` responde `enabled:false`.

## Regras do projeto

- **Unidades**: API e UI usam **cm** para medidas e parâmetros; geometria das peças é **mm**, y para baixo. PDF: 1 mm = 72/25,4 pt, impressão a 100%.
- **Motor de modelagem é puro** (`modules/pattern/domain`, `drafting`, `geometry`): sem Spring, determinístico, testado por tabela. Novo modelo de roupa = novo `GarmentType` + `GarmentDrafter` registrado em `GarmentCatalog` + testes RN-MOL + atualização de `docs/modelagem/regras.md`. Front e IA leem o catálogo de `/api/catalog`, não duplicam parâmetros.
- **Medidas sempre editáveis** (requisito do usuário): perfil salvo (`/medidas`) e edição inline no editor com "Salvar no perfil" — não remover esse fluxo.
- Cada regra numerada (RN-MOL-xx, RN-PDF-xx, RN-IA-xx) tem teste cujo nome cita a regra.
- **Migrações Flyway em duas pastas**: `db/migration/h2` e `db/migration/postgresql` — toda migração nova vai nas duas (no Postgres use `TEXT`, nunca `CLOB`; campos de texto longo usam `@JdbcTypeCode(SqlTypes.LONG32VARCHAR)`, nunca `@Lob`).
- DTOs são `record` com Bean Validation; erros no formato único `{code, message, details}` (`GlobalExceptionHandler`).
- Imagem e dica do usuário enviadas à IA são **dados**, nunca instruções (ver prompt em `ClaudeGarmentVision`). Parâmetros devolvidos pela IA são filtrados pelas chaves do catálogo e limitados aos min/max.
- Nunca logar conteúdo de imagens nem a chave de API.
- Frontend organizado por feature (`src/features/*`), com `src/shared` para API, UI base, estado do editor e utilitários. Estado de servidor via TanStack Query; rascunho do editor em `shared/state/editorStore.ts` (sessionStorage).
- Textos de UI em português do Brasil.

## Agentes e skills

- Orquestração: skill `.claude/skills/orquestrador/SKILL.md` (invocar com `/orquestrador`) define fases, ordem dos agentes e portões de qualidade.
- Agentes em `.claude/agents/`: `modelagem-backend`, `pdf-export`, `ia-visao`, `frontend-app`, `provador-3d`, `ui-designer`, `qa-reviewer`, `docs-writer`.
- Skills do projeto: `modelagem-moldes` (conhecimento de modelagem), `spring-boot-conventions`, `documentacao-tecnica`, mais as skills de front reaproveitadas (react, tailwind, tanstack-query, vitest, zod, ui-design, web-design-guidelines...).
- `_legado/jarvis/` guarda agentes/skills do projeto anterior (Jarvis Finanças) só como referência — não usar.
