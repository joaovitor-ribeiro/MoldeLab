---
name: spring-boot-conventions
description: Convenções do backend do MoldeLab (Java 21 + Spring Boot 3.5 + H2/Flyway + PDFBox + SDK Anthropic). Use sempre que for criar ou alterar código em backend/ — estrutura de módulos, DTOs, erros, persistência, unidades e testes.
---

# Convenções do backend (MoldeLab)

## Onde o código vive
`backend/`, Maven (`./mvnw`), pacote raiz `com.moldelab`. Módulos em `com.moldelab.modules.<modulo>`: `measurements`, `pattern`, `export`, `ai`, `projects`. Compartilhado em `com.moldelab.shared` (`ApiException`, `GlobalExceptionHandler`, `JsonCodec`, `AppConfig`).

## Estrutura
- **Lógica pura sem Spring** em pacotes `domain/`, `geometry/`, `drafting/` (pattern) e classes de render (export). Beans criados em `AppConfig` quando precisam ser injetados.
- Controllers finos com DTOs `record` aninhados + Bean Validation. Nunca expor entidade JPA; nunca `Map<String,Object>` como corpo de entrada genérico (exceção: `params` de modelo, que é validado pelo catálogo).
- Um módulo usa outro apenas por tipos públicos de `api/` (ex.: `measurements.api.Measurements`) ou pelo domínio puro do `pattern`. Não importar repositório/entidade de outro módulo.

## Persistência
- H2 em arquivo (`backend/data/`), modo PostgreSQL, `ddl-auto: validate`. Schema só por **Flyway** (`db/migration/V<n>__*.sql`); nunca editar migração já aplicada — criar a próxima.
- Campos flexíveis (medidas, parâmetros, margens) como JSON em `CLOB` via `JsonCodec`.
- App local de um usuário: não há autenticação no MVP. Se surgir multiusuário, filtrar tudo por usuário e buscar por `findByIdAndUserId`.

## Unidades
- API em **cm** (medidas, parâmetros, margens `seamCm/hemCm/finishCm`); domínio de geometria em **mm** (`double`). Conversão só na borda (`DraftInput.paramMm`, `Measurements.mm`, `SeamRequest.toSettings`).
- PDF em pontos: `mm × 72/25,4`. Texto do PDF passa por `PdfPatternRenderer.safe` (WinAnsi).

## Erros
- `ApiException` com `code` estável (lista em `docs/api-contracts.md`); exceções de domínio convertidas no controller (ex.: `InvalidMeasurementsException` → 422 `INVALID_MEASUREMENTS`). Nada de stack trace na resposta.
- Nunca logar imagens enviadas, base64 nem `ANTHROPIC_API_KEY`.

## IA
- Só via SDK oficial `com.anthropic:anthropic-java`; modelo em `moldelab.ai.model`; chave por ambiente. Saída estruturada por classe (`outputConfig(Classe.class)`), validação pós-resposta contra o catálogo. Testes nunca chamam a API (usar `GarmentVision` fake ou `moldelab.ai.enabled=false`).

## Testes
- Domínio: JUnit 5 + AssertJ, parametrizados por tamanho padrão/modelo, sem Spring.
- API: `@SpringBootTest` + MockMvc com H2 em memória (`ApiIntegrationTest`).
- Nome do teste cita a regra (`rnMol05_...`). Seguir a skill `tdd`.

## Pronto quando
- [ ] `./mvnw verify` verde
- [ ] Regras RN novas documentadas em `docs/modelagem/regras.md` e testadas
- [ ] `docs/api-contracts.md` atualizado se o contrato mudou
- [ ] Para geometria/PDF: resultado inspecionado visualmente
