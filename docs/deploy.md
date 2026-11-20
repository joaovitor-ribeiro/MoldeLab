# Deploy — Google Cloud Run + PostgreSQL gratuito

Uma imagem só (`Dockerfile` na raiz): o Spring Boot serve a API em `/api` e o frontend (build do Vite) na mesma origem — sem CORS, sem segundo serviço.

| Peça | Onde | Custo |
|---|---|---|
| App (API + front) | Google Cloud Run | camada gratuita (2 mi req/mês; escala a zero) |
| Banco | Neon (PostgreSQL) — recomendado | grátis, 0,5 GB |
| Chave da IA (opcional) | Secret Manager | grátis até 6 versões ativas |

## Como o backend escolhe o banco

Por variáveis de ambiente (`application.yml`):

| Variável | Exemplo | Sem ela |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://ep-xxx.sa-east-1.aws.neon.tech/neondb?sslmode=require` | H2 em arquivo (`backend/data`, dev local) |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `neondb_owner` / `***` | `sa` / vazio |
| `DATABASE_POOL_SIZE` | `5` | `5` |
| `ANTHROPIC_API_KEY` | `sk-ant-...` | IA desligada (escolha manual do modelo) |
| `CORS_ALLOWED_ORIGINS` | só se o front ficar em outro domínio | localhost:5173 |
| `PORT` | definido pelo Cloud Run | 8080 |

As tabelas são criadas pelo Flyway na primeira subida (`db/migration/postgresql`). Migrações H2 (dev) ficam em `db/migration/h2` — **toda migração nova precisa existir nas duas pastas**.

## 1. Banco no Neon

1. Crie conta em neon.tech → *New project* → região **AWS São Paulo (sa-east-1)** se disponível (ou a mais próxima da região do Cloud Run).
2. Em *Connection details* escolha **Java/JDBC** e desmarque *Connection pooling* (o Flyway precisa da conexão direta; o pool do Hikari já basta).
3. Anote host, banco, usuário e senha. A URL fica `jdbc:postgresql://<host>/<banco>?sslmode=require`.

> O Neon "dorme" após 5 min sem uso; a primeira requisição depois disso leva ~1 s a mais. Alternativas: Supabase (500 MB, pausa o projeto após 7 dias sem acesso), Aiven (plano free), ou MongoDB Atlas M0 (exigiria reescrever a persistência — não recomendado).

## 2. Cloud Run pelo console (console.cloud.google.com)

1. Crie/escolha um projeto e ative faturamento (exigido mesmo na camada gratuita).
2. *Secret Manager* → crie `moldelab-db-password` (senha do Neon) e, se for usar IA, `anthropic-api-key`.
3. *Cloud Run* → **Implantar contêiner** → **Implantar continuamente de um repositório** → conecte o GitHub `joaovitor-ribeiro/MoldeLab`, branch `main`, tipo de build **Dockerfile** (caminho `/Dockerfile`).
4. Configuração do serviço:
   - Região: `southamerica-east1` (São Paulo).
   - Autenticação: **Permitir acesso público**.
   - Faturamento: *baseado em solicitação*; instâncias mín. **0**, máx. **2** (protege a cota grátis).
   - Memória **1 GiB**, CPU 1; tempo limite 120 s (análise por IA pode levar até 90 s).
   - *Variáveis e secrets*: `DATABASE_URL`, `DATABASE_USERNAME` como variáveis; `DATABASE_PASSWORD` ← secret `moldelab-db-password`; `ANTHROPIC_API_KEY` ← secret `anthropic-api-key` (opcional).
   - Na conta de serviço do Cloud Run, dê o papel **Secret Manager Secret Accessor**.
5. Implante. A URL `https://moldelab-xxxx.a.run.app` já serve o app. Saúde: `/actuator/health`.

Cada `git push` na `main` gera um build novo (Cloud Build tem 120 min/dia grátis).

## Testar a imagem localmente

```bash
docker build -t moldelab .
docker run --rm -p 8080:8080 moldelab                 # com H2 dentro do contêiner (dados somem ao parar)
docker run --rm -p 8080:8080 -e DATABASE_URL=... -e DATABASE_USERNAME=... -e DATABASE_PASSWORD=... moldelab
```

## Atenção

- **Espaço**: as imagens de referência dos projetos ficam no banco (até ~4,5 MB cada). Com 0,5 GB cabem ~100–500 projetos com foto. Se apertar, o próximo passo é mover as imagens para o Cloud Storage (5 GB grátis).
- **Sem login ainda**: qualquer pessoa com a URL vê e edita os perfis/projetos. Ok para uso pessoal; antes de divulgar, adicionar autenticação (está no roadmap).
- A chave da IA tem custo por uso na Anthropic; sem login, qualquer visitante pode gastá-la.
