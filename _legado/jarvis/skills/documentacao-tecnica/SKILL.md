---
name: documentacao-tecnica
description: Use esta skill sempre que for gerar ou atualizar a documentação técnica de um domínio/módulo do Jarvis Finanças (auth, incomes, expenses, summary, import, assistant). Garante que todos os módulos documentem no mesmo formato, facilitando integração entre agentes e revisão humana.
---

# Skill: documentação técnica por domínio

## Quando usar

Sempre que um agente gerar ou atualizar a documentação de um módulo. É documentação técnica para desenvolvedores e outros agentes, não para o usuário final.

## Template obrigatório (`docs/<dominio>/overview.md`)

```markdown
# Domínio: <nome>

## 1. Responsabilidade
O que o módulo faz e, igualmente importante, o que NÃO faz (limites explícitos).

## 2. Entidades
Para cada entidade: nome, campos principais, tipo, obrigatoriedade e relações (referenciar data-model.md).

## 3. Regras de negócio
Lista objetiva, numerada com prefixo do domínio (RN-EXP-01, RN-INC-02...). Uma frase, sem ambiguidade, verificável.
Exemplo: "RN-EXP-04: a última parcela absorve a diferença de centavos, e a soma das parcelas é exatamente o total."

## 4. Eventos publicados
N/A neste projeto (sem mensageria), salvo decisão futura documentada.

## 5. Eventos consumidos
N/A (idem).

## 6. Endpoints
Método, rota, request, response, códigos de erro (referenciar api-contracts.md).

## 7. Casos de borda conhecidos
Situações não óbvias já pensadas (ex.: compra dia 31 com fechamento dia 28, valor 0,10 em 3x).

## 8. Dependências externas
Bibliotecas críticas e serviços de terceiros (ex.: Apache POI, API da Anthropic).

## Pendências
O que ainda não foi decidido ou implementado.
```

## Regras de conteúdo

- Regras de negócio **verificáveis**. Evite "o sistema deve tratar bem os erros"; prefira "RN-IMP-05: confirmar o mesmo batch duas vezes retorna 409".
- Toda entidade/campo novo deve existir também em `docs/architecture/data-model.md`; o documento de domínio referencia o modelo central, não o duplica.
- Todo endpoint documentado deve bater exatamente com `docs/architecture/api-contracts.md`. Divergência é bug de documentação — corrigir imediatamente.
- Decisões de UI/visual **não** entram aqui (vivem em `docs/design/ui-ux.md`).
- Sempre terminar com `## Pendências`, honesta, para não passar falsa impressão de completude.

## Checklist

- [ ] Todas as 8 seções + Pendências presentes (N/A quando não se aplica)
- [ ] Toda entidade citada existe em `data-model.md`
- [ ] Todo endpoint citado existe em `api-contracts.md`
- [ ] Regras testáveis e numeradas; cada uma com teste correspondente no código
- [ ] Pendências preenchidas honestamente
