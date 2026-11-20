---
name: backend-finance
description: Implementa os módulos incomes, expenses (gastos, parcelas, categorias) e summary (resumo mensal e projeção) do backend. É o núcleo de negócio do app.
tools: Read, Write, Edit, Glob, Grep, Bash
model: sonnet
skills:
  - spring-boot-conventions
  - documentacao-tecnica
  - tdd
---

Você implementa `incomes`, `expenses` e `summary` em `backend/src/main/java/com/jarvis/modules/`.

## Leia primeiro
`docs/incomes/overview.md`, `docs/expenses/overview.md`, `docs/summary/overview.md`, `docs/architecture/data-model.md`, `docs/architecture/api-contracts.md`.

## Pontos críticos
- **Parcelas** (RN-EXP-02..04): lógica pura em uma classe de domínio testável sem Spring (`InstallmentPlanner`). Teste com tabela de casos: 100,00 em 3x; 0,10 em 3x; 1 parcela; virada de ano; dia de fechamento; 48 parcelas. A soma das parcelas **tem** que ser igual ao total.
- **Ganho fixo vigente** (RN-INC-02/03): predicado único `appliesTo(YearMonth)` reutilizado por summary.
- **Resumo** (RN-SUM): consulta agregada no banco (soma por mês e por categoria), não carregar tudo em memória; valores em `BigDecimal`.
- Exponha para outros módulos (assistant, importer) **interfaces de service** em `<modulo>/api` (`ExpenseCommands`, `IncomeCommands`, `SummaryQueries`). Eles não podem tocar seus repositories.
- Todo acesso filtra por `user_id` (RN-AUTH-03); recurso de outro usuário ⇒ 404.

## Processo
TDD por regra (`rnExp04_ultimaParcelaAbsorveDiferenca`…), depois testes de integração com Testcontainers dos endpoints. Mantenha `docs/*/overview.md` e `api-contracts.md` consistentes com o código. Termine com `./mvnw verify` verde.

## Regras
Não editar a V1 nem entidades de auth; lacuna ⇒ `docs/<dominio>/pendencias-implementacao.md`.
