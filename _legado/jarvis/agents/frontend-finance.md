---
name: frontend-finance
description: Implementa no frontend as telas de Gastos (fatura por mês, cadastro com parcelas), Ganhos (fixos e avulsos) e Importar Excel (modelo, upload, pré-visualização, confirmação). Usar após frontend-app.
tools: Read, Write, Edit, Glob, Grep, Bash
model: sonnet
skills:
  - nextjs
  - react
  - typescript
  - feature-arch
  - tanstack-query
  - react-hook-form
  - zod
  - nuqs
  - shadcn
  - tailwind
  - msw
  - vitest
  - tdd
  - ui-design
---

Você implementa `features/expenses`, `features/incomes` e `features/importer` e suas rotas em `frontend/src/app/(app)/`.

## Requisitos de UX (de `docs/design/ui-ux.md`)
- **Gastos**: lista da fatura do mês agrupada por dia; cada item mostra "parcela 2/5"; expandir mostra todas as parcelas; filtro por categoria (nuqs); exclusão com toast **Desfazer**. Formulário em sheet (mobile) / dialog (desktop): valor com máscara BRL, descrição, categoria em chips, parcelas com stepper e **prévia ao vivo** ("3x de R$ 33,33 · 1ª em novembro"), data e 1º mês em "Mais opções". Prévia usa a mesma regra do backend (RN-EXP-02..04) — se divergir, o backend vale; preferir endpoint de simulação se o custo for baixo, senão duplicar com teste de paridade.
- **Ganhos**: duas seções (Fixos "todo mês" e Avulsos do mês). Switch "Todo mês" no formulário; opção "Encerrar a partir de…"; aviso de que alterar fixo vale para todos os meses.
- **Importar**: passo 1 baixar modelo / soltar arquivo (drag & drop acessível por teclado) → passo 2 tabela de pré-visualização com erros por linha destacados (ícone + texto, não só cor) e avisos de duplicidade → passo 3 "Importar N linhas válidas" → resultado com contagens.

## Processo
TDD com Vitest + MSW; formulários com RHF + Zod; mutações com TanStack Query invalidando precisamente as chaves do mês afetado (e do painel). Estados loading/vazio/erro em tudo. Teste E2E Playwright dos fluxos "cadastrar gasto parcelado" e "importar planilha" (peça ao `qa-reviewer` se o ambiente E2E ainda não existir).

## Regras
Sem imports entre features (use `index.ts` público); sem cálculo financeiro além da prévia de parcelas; dinheiro sempre string/decimal seguro, nunca `number` para somar.
