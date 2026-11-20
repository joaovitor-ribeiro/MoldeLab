---
name: spring-boot-conventions
description: Use esta skill sempre que for implementar código de um módulo do backend do Jarvis Finanças (Java 21 + Spring Boot + PostgreSQL). Garante consistência de estrutura, segurança por usuário, tratamento de erro, dinheiro/datas e testes entre módulos escritos por agentes diferentes.
---

# Skill: convenções de implementação Spring Boot (Jarvis Finanças)

## Onde o código vive

Backend em `backend/` (Maven, pacote raiz `com.jarvis`), na raiz do projeto `jarvis-financas`. Documentação em `docs/`. Ver `CLAUDE.md`.

## ORM e schema

**Spring Data JPA + Hibernate**; `ddl-auto: validate`. O schema é versionado por **Flyway** (`src/main/resources/db/migration`). A baseline `V1__init.sql` e as entidades iniciais são geradas pelo agente `setup-backend` a partir de `docs/architecture/data-model.md`.

Agentes de domínio **não editam entidades de outro módulo nem a V1**. Se faltar campo/tabela, parem e registrem em `docs/<dominio>/pendencias-implementacao.md`. Novas migrações (`V2__...sql`) só de forma coordenada, nunca por dois agentes na mesma rodada paralela.

## Estrutura por módulo

```
src/main/java/com/jarvis/modules/<modulo>/
  api/            interfaces públicas usadas por OUTROS módulos (ex.: ExpenseCommands)
  controller/     <Recurso>Controller   (fino: valida, delega, mapeia status)
  service/        <Recurso>Service      (regras de negócio, @Transactional)
  dto/            records: Create<Recurso>Request, <Recurso>Response...
  entity/         <Entidade>
  repository/     <Entidade>Repository  (Spring Data)
  domain/         lógica pura sem Spring (ex.: InstallmentPlanner), fácil de testar

src/test/java/com/jarvis/modules/<modulo>/
  <Classe>Test.java              unitários (JUnit 5 + Mockito; domain/ sem mocks)
  <Recurso>IntegrationTest.java  MockMvc + Testcontainers PostgreSQL
```

Component scan descobre tudo dentro de `com.jarvis.*`; não há registro manual.

## Fronteiras entre módulos

- Dependências permitidas: ver `docs/architecture/architecture.md`. Sem ciclos.
- Um módulo só chama outro pelas **interfaces em `api/`**; nunca importa `repository` ou `entity` de outro módulo.

## Segurança e isolamento

- Todo acesso a dados filtra por `userId` do token (`CurrentUser`). Buscar por id sempre como `findByIdAndUserId`. Recurso de outro usuário ⇒ `NotFoundException` (404).
- Nunca logar senha, token, chave de API ou conteúdo de planilha. Mensagens de erro de autenticação são genéricas.

## Dinheiro e datas

- `BigDecimal` em todo lugar (nunca `double`/`float`); `NUMERIC(14,2)`; escala 2. Rateio de parcelas via helper `Money` (última parcela absorve a diferença).
- JSON: dinheiro como string decimal; mês `YYYY-MM`; data ISO. Fuso `America/Sao_Paulo`; injetar `Clock` para testar "hoje".

## DTOs e validação

- Todo endpoint recebe e devolve DTO (`record`) com Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@Size`, `@Digits`). Nunca `Map`/`Object` no controller; nunca expor entidade JPA na API.
- Cada regra de negócio numerada (RN-XX) dos `docs/<dominio>/overview.md` tem um teste cujo nome cita a regra (ex.: `rnExp04_ultimaParcelaAbsorveDiferenca()`).

## Tratamento de erro

- Exceções de domínio (`BusinessRuleException`, `NotFoundException`...) mapeadas no `@RestControllerAdvice` para o formato único de `api-contracts.md`. Nada de 500 sem contexto; nunca vazar stack trace.
- Casos de borda documentados têm tratamento explícito no código.

## Testes

- Services: unitário. Lógica de `domain/`: testes de tabela (parametrizados) sem Spring.
- Repositórios e endpoints: integração com Testcontainers (PostgreSQL real, mesma versão do compose).
- Nenhum teste depende de rede, relógio real ou chave de API externa.
- Siga a skill `tdd` (red → green → refactor).

## Antes de considerar um módulo pronto

- [ ] Código segue `data-model.md` e `api-contracts.md`, sem campos inventados
- [ ] Toda RN-XX do `overview.md` tem código e teste
- [ ] Isolamento por usuário testado (usuário A não acessa dados de B)
- [ ] Sem import de repository/entity de outro módulo
- [ ] `./mvnw verify` verde
- [ ] `docs/<dominio>/overview.md` atualizado (inclui Pendências)
