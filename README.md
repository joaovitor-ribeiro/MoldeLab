# MoldeLab

Desenhe ou fotografe uma roupa → ajuste com as suas medidas → veja no provador 3D → baixe o molde em PDF tamanho real → imprima, monte, corte e costure.

## Rodar

Pré-requisitos: Java 21, Node 20+ (Maven vem pelo `mvnw`).

```powershell
.\start.ps1        # ou dois cliques em start.cmd
```

Abre http://localhost:5173 (frontend) com a API em http://localhost:8080.

Manual:
```bash
cd backend  && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

### Leitura de imagem por IA (opcional)
Defina `ANTHROPIC_API_KEY` no terminal antes de subir o backend. Sem ela, o app funciona normalmente e você escolhe o modelo manualmente.

## Testes
```bash
cd backend  && ./mvnw verify
cd frontend && npm test && npm run typecheck && npm run lint && npm run build
```

## Imprimir o molde
1. Imprima a **100% / tamanho real** (desligue "ajustar à página").
2. Meça o quadrado de teste: **5 × 5 cm**.
3. Monte as folhas pelo mapa da capa, alinhando os losangos.
4. Recorte pela linha contínua; a tracejada é a costura. Peça com linha azul vai na dobra do tecido.

Documentação: [`CLAUDE.md`](CLAUDE.md) · [`docs/`](docs/).
