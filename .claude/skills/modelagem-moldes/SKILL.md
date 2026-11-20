---
name: modelagem-moldes
description: Conhecimento de modelagem plana e convenções do motor do MoldeLab (unidades, arestas, margens, piques, fio, folgas, fórmulas de base). Use ao criar ou alterar drafters, regras RN-MOL, layout do PDF ou a interpretação de parâmetros pela IA/3D.
---

# Modelagem plana no MoldeLab

## Convenções do motor
- Coordenadas em **mm**, x→direita, **y→baixo**. Peça descrita pela **linha de costura** como lista de `Edge` consecutivas (fim de uma = início da próxima; fecha na primeira). Orientação livre — `SeamAllowance` detecta pela área com sinal.
- Tipos de aresta: `SEAM` (costura), `HEM` (barra), `FOLD` (dobra, margem 0), `FINISH` (decote/cava com acabamento). A linha de corte é gerada — nunca desenhe margem à mão.
- Peça "na dobra": desenhe só a metade, com a aresta central `FOLD`; `cutQuantity` conta as peças finais.
- Piques: `notch(edgeName, distância desde o início)` ou `notchFromEnd`. Use 1 pique = frente, 2 piques = costas (convenção de manga/cava).
- Fio: padrão vertical (paralelo à dobra/centro). Defina explicitamente quando a peça é rotacionada (cós, godê).
- Curvas: `Curves.cubic`, `Curves.arc`, `Curves.smoothThrough` (passa por pontos). Comprimentos com `Curves.length` — use-os para casar costuras (cabeça da manga × cava, ribana × decote, cós × cintura).
- Parâmetros: declarar em `params()` com unidade de exibição (cm, %, opções). `DraftInput.paramMm` converte cm→mm já limitado.
- Avisos (`in.warn`) em português, acionáveis ("diminua o comprimento ou...").

## Folgas de referência (vestibilidade)
| Peça | Folga no contorno |
|---|---|
| Malha justa | 0 a −5 cm (negativa em malha com elastano) |
| Camiseta básica | 4–10 cm |
| Blusa tecido plano | 8–12 cm no busto |
| Saia reta | 2–4 cm quadril, 1 cm cintura |
| Calça | 4–6 cm quadril |

## Checklist de um drafter novo
- [ ] Medidas obrigatórias mínimas em `requiredMeasurements()`.
- [ ] Costuras que se unem têm o mesmo comprimento (teste numérico com tolerância ≤ 1 mm, ou folga intencional documentada).
- [ ] Peças válidas para todos os `StandardSizes` (teste geral já cobre ao registrar no catálogo).
- [ ] Casos de borda viram avisos, não exceções.
- [ ] RN-MOL documentada em `docs/modelagem/regras.md` com o nome do teste.
- [ ] Ver o resultado: gerar PDF e renderizar a página em PNG (PDFBox `PDFRenderer`) e olhar.
