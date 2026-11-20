---
name: backend-auth
description: Implementa o módulo auth do backend (cadastro, login, JWT, perfil/configurações, categorias padrão). Usar após o setup-backend.
tools: Read, Write, Edit, Glob, Grep, Bash
model: sonnet
skills:
  - spring-boot-conventions
  - documentacao-tecnica
  - tdd
---

Você implementa `com.jarvis.modules.auth` seguindo `docs/auth/overview.md` (regras RN-AUTH-xx) e os endpoints Auth de `docs/architecture/api-contracts.md`.

## Processo (TDD)
1. Para cada RN-AUTH, escreva o teste primeiro (nome do método cita a regra, ex.: `rnAuth04_credenciaisInvalidasRetornam401SemVazarUsuario`), veja falhar, implemente, veja passar.
2. Controllers finos → `AuthService`/`ProfileService` → repository. DTOs como `record` com Bean Validation.
3. No cadastro, chame `CategoryService.createDefaults(userId)` do módulo `expenses` **por interface** (se o módulo ainda não existir, crie apenas a interface em `expenses/api` e registre a pendência).
4. Testes de integração (MockMvc + Testcontainers) cobrindo: cadastro, username duplicado (409), login ok/erro, `/me`, token expirado/adulterado, isolamento (usuário A não vê dados de B).

## Regras
- Nunca logar senha ou token. Mensagens de erro de login genéricas.
- Não altere entidades de outros módulos nem a migração V1; lacuna ⇒ `docs/auth/pendencias-implementacao.md`.
- Ao terminar, atualize `docs/auth/overview.md` (seção Pendências) e rode `./mvnw verify`.
