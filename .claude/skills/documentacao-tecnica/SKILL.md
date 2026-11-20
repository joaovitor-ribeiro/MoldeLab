---
name: documentacao-tecnica
description: Use esta skill sempre que for gerar ou atualizar a documentação técnica de um domínio/módulo do MoldeLab (measurements, pattern, export, ai, projects). Garante que todos os módulos documentem no mesmo formato, facilitando integração entre agentes e revisão humana.
---

# Skill: documentação técnica por domínio

## Quando usar

Sempre que um agente gerar ou atualizar a documentação de um módulo. É documentação técnica para desenvolvedores e outros agentes, não para o usuário final.

## Template obrigatório (`docs/<dominio>/regras.md` (ou overview.md))

```markdown
# Domínio: <nome>

## 1. Responsabilidade
O que o módulo faz e, igualmente importante, o que NÃO faz (limites explícitos).

## 2. Entidades
Para cada entidade: nome, campos principais, tipo, obrigatoriedade e relações (referenciar data-model.md).

## 3. Regras de negócio
Lista objetiva, numerada com prefixo do domínio (RN-MOL-01, RN-PDF-02...). Uma frase, sem ambiguidade, verificável.
Exemplo: "RN-MOL-05: a cabeça da manga tem o comprimento das cavas + 0,5 cm."

## 4. Eventos publicados
N/A neste projeto (sem mensageria), salvo decisão futura documentada.

## 5. Eventos consumidos
N/A (idem).

## 6. Endpoints
Método, rota, request, response, códigos de erro (referenciar api-contracts.md).

## 7. Casos de borda conhecidos
Situações não óbvias já pensadas (ex.: cintura maior que o quadril, godê que não cabe no tecido).

## 8. Dependências externas
Bibliotecas críticas e serviços de terceiros (ex.: PDFBox, SDK da Anthropic).

## Pendências
O que ainda não foi decidido ou implementado.
```

## Regras de conteúdo

- Regras de negócio **verificáveis**. Evite "o sistema deve tratar bem os erros"; prefira "RN-MOL-08: cintura de 7 cm é rejeitada com 422".
- Toda entidade/campo novo deve existir também em `backend/src/main/resources/db/migration`; o documento de domínio referencia o modelo central, não o duplica.
- Todo endpoint documentado deve bater exatamente com `docs/api-contracts.md`. Divergência é bug de documentação — corrigir imediatamente.
- Decisões de UI/visual **não** entram aqui (vivem em `docs/arquitetura.md` / componentes de UI).
- Sempre terminar com `## Pendências`, honesta, para não passar falsa impressão de completude.

## Checklist

- [ ] Todas as 8 seções + Pendências presentes (N/A quando não se aplica)
- [ ] Toda entidade citada existe em a migração Flyway
- [ ] Todo endpoint citado existe em `docs/api-contracts.md`
- [ ] Regras testáveis e numeradas; cada uma com teste correspondente no código
- [ ] Pendências preenchidas honestamente
