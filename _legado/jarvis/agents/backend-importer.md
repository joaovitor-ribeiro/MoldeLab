---
name: backend-importer
description: Implementa o módulo importer do backend (upload de Excel .xlsx, modelo para download, pré-visualização com erros por linha e confirmação). Usar após backend-finance.
tools: Read, Write, Edit, Glob, Grep, Bash
model: sonnet
skills:
  - spring-boot-conventions
  - documentacao-tecnica
  - tdd
---

Você implementa `com.jarvis.modules.importer` conforme `docs/import/overview.md` (RN-IMP-xx) e a seção Importação de `api-contracts.md`.

## Entregas
1. `GET /import/template`: gera `modelo-jarvis.xlsx` com POI (aba `Lançamentos`, cabeçalho estilizado, 3 linhas de exemplo, validação de dados em `Tipo` e `Fixo`).
2. `POST /import/preview`: valida arquivo (assinatura ZIP/OOXML, tamanho, nº de linhas), lê as linhas, valida cada uma **chamando os mesmos validadores/services de `expenses`/`incomes`** (via interfaces `*Commands`/`*Validator` em `<modulo>/api`; não duplique regra), salva `import_batches` e devolve o preview.
3. `POST /import/{id}/confirm`: transação única, só linhas válidas, idempotente (RN-IMP-05).
4. `DELETE /import/{id}`.

## Testes
Gere .xlsx de teste em memória com POI (sem arquivos binários no repo). Cubra: arquivo válido, extensão falsa (.xlsx que não é zip), > 1000 linhas, vírgula/ponto decimal, datas Excel e texto, linha com erro misturada com válidas, confirmação dupla (409), batch de outro usuário (404), categoria nova criada.

## Segurança
Nunca avaliar macros/fórmulas externas; limitar tamanho e linhas; não confiar no `Content-Type`; não gravar o arquivo em disco.
