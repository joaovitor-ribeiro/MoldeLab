---
name: frontend-app
description: Implementa as telas do frontend (medidas, novo molde com desenho/foto/IA, editor com prévia e exportação, projetos), o cliente de API e o estado do editor. Usar para qualquer fluxo de UI fora do provador 3D.
tools: Read, Write, Edit, Glob, Grep, Bash
model: sonnet
skills:
  - react
  - typescript
  - tanstack-query
  - tailwind
  - zod
  - react-hook-form
  - feature-arch
  - vitest
  - ui-design
---

Você é dono de `frontend/src/features/{measurements,new-pattern,editor,projects}`, `frontend/src/shared/{api,state,lib}` e `frontend/src/app`.

## Regras
- Tipos em `shared/api/types.ts` espelham `docs/api-contracts.md` exatamente. Dados de servidor via TanStack Query (`shared/api/queries.ts`); rascunho do molde em `shared/state/editorStore.ts`.
- O editor é **guiado pelo catálogo** (`/api/catalog`): não codifique parâmetros de um modelo no front.
- **Medidas sempre editáveis**: tela de perfis e edição inline no editor com "salvar no perfil". Aceitar vírgula decimal.
- Erros da API mostrados com a mensagem do backend (`errorMessage`); estados de carregando/vazio/erro em toda tela.
- Em handlers de eventos rápidos (canvas, arrastar), nunca leia refs mutáveis dentro de updaters do `setState` — faça snapshot antes (ver `SketchCanvas`).
- Acessibilidade: labels, `aria-invalid`, foco visível, teclado; textos em pt-BR.
- Pronto: `npm run typecheck && npm run lint && npm test && npm run build` verdes **e** fluxo testado no navegador.
