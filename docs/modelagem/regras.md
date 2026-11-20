# Domínio: modelagem e PDF

## 1. Responsabilidade
Transformar medidas (cm) + modelo + parâmetros em peças de molde planas (mm) com margens, piques, fio e dobra, e exportá-las em PDF de escala real. **Não** faz: simulação de caimento, gradação, encaixe otimizado (ver roadmap).

## 2. Entidades
Sem persistência própria. Objetos de valor: `Edge`, `PatternPiece`, `Pattern`, `SeamSettings`, `ParamSpec`, `GarmentType`. Medidas em `Measurements` (ver api-contracts).

## 3. Regras de negócio
| Regra | Descrição | Teste |
|---|---|---|
| RN-MOL-01 | Cada borda recebe a margem do seu tipo: costura (padrão 1 cm), barra (2,5 cm), acabamento (0,7 cm). | `SeamAllowanceTest.rnMol01_*` |
| RN-MOL-02 | Borda na dobra do tecido tem margem zero e é marcada "colocar na dobra". | `SeamAllowanceTest.rnMol02_*` |
| RN-MOL-03 | Saia reta: largura no quadril = (quadril + folga)/4 ± 0,5 cm (frente maior que costas); excesso cintura→quadril vai ~45% para a lateral (máx. 3 cm) e o resto em pences (1 por peça até 3 cm, senão 2). | `DraftersTest.rnMol03_*` |
| RN-MOL-04 | Saia godê: raio da cintura = (cintura + 1 cm)/(2π·fração); a cintura das peças soma exatamente a medida + folga. | `DraftersTest.rnMol04_*` |
| RN-MOL-05 | Manga: a cabeça tem o comprimento das cavas (frente + costas) + 0,5 cm de folga (altura da cabeça por busca binária). | `DraftersTest.rnMol05_*` |
| RN-MOL-06 | Ribana do decote = 85% do comprimento total do decote. | `DraftersTest.rnMol06_*` |
| RN-MOL-07 | Medidas obrigatórias ausentes impedem o molde e são listadas por nome. | `DraftersTest.rnMol07_*`, `ApiIntegrationTest.draftSemMedidas*` |
| RN-MOL-08 | Medida fora do intervalo plausível (ex.: cintura 7 cm) é rejeitada. | `DraftersTest.rnMol08_*` |
| RN-MOL-09 | Parâmetro fora do limite do modelo é ajustado ao limite com aviso; parâmetro desconhecido é ignorado com aviso. | `DraftersTest.rnMol09_*` |
| RN-MOL-10 | Toda peça é válida: área > 0, linha de corte envolve a de costura, rótulo e fio dentro da peça. Vale para todos os tamanhos padrão × todos os modelos. | `DraftersTest.todaPecaEhGeometricamenteValida` |
| RN-PDF-01 | PDF em escala 1:1 com páginas do tamanho do papel escolhido, capa com instruções e quadrado de teste de 5 × 5 cm. | `PdfPatternRendererTest.rnPdf01_*` |
| RN-PDF-02 | Plotter: uma faixa única com a largura do rolo. | `PdfPatternRendererTest.rnPdf02_*` |
| RN-PDF-03 | Folhas sem nenhuma peça não são impressas; todas as peças cabem na folha virtual. | `PdfPatternRendererTest.rnPdf03_*` |
| RN-IA-01 | Parâmetros sugeridos pela IA com chave desconhecida são descartados; valores são limitados ao min/max do catálogo. | `AiControllerTest.rnIa01_*` |
| RN-IA-02 | Modelo "não suportado" não traz parâmetros. | `AiControllerTest.rnIa02_*` |
| RN-IA-03 | Tipo de imagem é detectado pelos bytes, não pelo Content-Type. | `AiControllerTest.rnIa03_*` |

## 4/5. Eventos
N/A.

## 6. Endpoints
`/api/catalog`, `/api/patterns/draft`, `/api/patterns/pdf`, `/api/ai/*` — ver `docs/api-contracts.md`.

## 7. Casos de borda conhecidos
- Cintura maior que o quadril (barriga): sem pences, lateral abre para fora.
- Saia mais curta que a altura do quadril + 5 cm: comprimento ajustado com aviso.
- Godê que não cabe em tecido de 1,50 m dobrado: aviso com a largura da peça.
- Contorno do braço muito grande para a cava: largura da manga reduzida com aviso.
- Ombro estreito para o pescoço: ombro mínimo de 6 cm com aviso.
- Cantos muito agudos na margem de costura são chanfrados (limite 3× a maior margem).

## 8. Dependências externas
Apache PDFBox 3 (PDF), SDK `com.anthropic:anthropic-java` (IA).

## Fórmulas (referência rápida)
- Camiseta: largura busto/4 com folga; decote largura = pescoço/6 + 0,5 + alargar; queda de ombro 4,5 cm; profundidade de cava = busto/6 + 4,5 + folga/4; decote costas 2 cm.
- Saia reta: cintura/4 + 0,25 (folga) ± 0,5 (equilíbrio); subida lateral 1 cm quando há modelagem lateral; pences de 9 cm (frente) / 13 cm (costas), limitadas à altura do quadril − 3 cm.

## Pendências
- Pences de busto na blusa (hoje é modelagem de malha sem pences).
- Barra curva compensada no evasê da camiseta.
- Validação com protótipos físicos (provas reais) para calibrar folgas e cava.
