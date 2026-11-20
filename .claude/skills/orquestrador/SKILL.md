---
name: orquestrador
description: Orquestra o desenvolvimento do MoldeLab — decide a próxima fase/tarefa a partir do roadmap, delega aos agentes do projeto na ordem certa (com paralelismo seguro), aplica os portões de qualidade e mantém docs e roadmap atualizados. Use quando o usuário pedir "continue o projeto", "próxima fase", "implemente o item X do roadmap" ou invocar /orquestrador.
---

# Orquestrador do MoldeLab

Você (sessão principal) coordena; os agentes executam. Subagentes não chamam outros subagentes — toda delegação parte daqui.

## 1. Entender o pedido
1. Leia `CLAUDE.md`, `docs/roadmap.md` e o item pedido (ou o próximo `[ ]` da versão atual).
2. Quebre em tarefas por **dono** (tabela abaixo). Se a tarefa muda contrato HTTP, atualize primeiro `docs/api-contracts.md` (você mesmo ou `docs-writer`) — contrato antes de código.
3. Tarefa pequena (1 arquivo, < 30 min)? Faça direto, sem delegar.

## 2. Donos
| Agente | Dono de |
|---|---|
| `modelagem-backend` | `modules/pattern/**` (geometria, drafters, catálogo, engine), `modules/measurements`, `modules/projects` |
| `pdf-export` | `modules/export/**` (layout de folhas, PDF, futuros SVG/DXF) |
| `ia-visao` | `modules/ai/**` (prompts, saída estruturada, medidas por foto) |
| `ui-designer` | tokens/tema (`index.css`), `shared/ui/**`, revisão visual/acessibilidade |
| `frontend-app` | `features/{measurements,new-pattern,editor,projects}`, `shared/api`, `shared/state` |
| `provador-3d` | `features/try-on/**` |
| `qa-reviewer` | builds, testes, E2E, auditoria contrato × código × docs |
| `docs-writer` | `docs/**`, `CLAUDE.md` |

## 3. Ordem e paralelismo
```
contrato (docs) ─▶ modelagem-backend ─┬─▶ pdf-export          (paralelo: pastas disjuntas)
                                      ├─▶ ia-visao            (se o catálogo mudou)
                                      └─▶ frontend-app ─▶ provador-3d (se novo modelo/parâmetro)
ui-designer pode rodar em paralelo a qualquer etapa de backend.
Sempre por último: qa-reviewer ─▶ docs-writer
```
- Rode em paralelo só agentes com pastas disjuntas. Nunca dois agentes editando a mesma pasta ou uma nova migração Flyway (`V2__...`) ao mesmo tempo.
- Ao delegar, passe: objetivo, arquivos/contratos relevantes, regras RN a criar/alterar, critério de pronto e "não mexer fora da sua pasta; pendências vão em `docs/pendencias.md`".

## 4. Receita: novo modelo de roupa
1. `modelagem-backend`: `GarmentType` + `GarmentDrafter` + registro no `GarmentCatalog`; RN-MOL novas com testes; o teste geral `todaPecaEhGeometricamenteValida` precisa passar para todos os tamanhos.
2. Em paralelo: `ia-visao` (enum `AnalyzedType`, conferir prompt — o catálogo entra sozinho) e `provador-3d` (`buildGarment` para o tipo novo).
3. `frontend-app`: só se houver UI específica (o editor é guiado pelo catálogo).
4. `qa-reviewer`, depois `docs-writer` (regras.md, roadmap, api-contracts se o enum mudou).

## 5. Portões de qualidade (não avance sem)
- `cd backend && ./mvnw verify` verde.
- `cd frontend && npm run typecheck && npm run lint && npm test && npm run build` verdes.
- Toda RN nova tem teste com o nome da regra.
- Para mudanças visuais/PDF: inspeção visual real (PDF renderizado em PNG ou o app no navegador), não só testes.
- `docs/api-contracts.md` bate com os controllers; roadmap marcado.
- Relate ao usuário com honestidade: o que foi feito, o que foi verificado e como, o que falhou ou ficou pendente.
