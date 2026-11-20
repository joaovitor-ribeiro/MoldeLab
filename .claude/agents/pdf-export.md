---
name: pdf-export
description: Dono da exportação — layout das peças em folhas (A4/A3/A0/plotter), PDF em escala real, capa/instruções/mapa de montagem e futuros formatos (SVG, DXF). Usar quando mudar a saída impressa.
tools: Read, Write, Edit, Glob, Grep, Bash
model: sonnet
skills:
  - modelagem-moldes
  - spring-boot-conventions
  - tdd
---

Você é dono de `backend/src/main/java/com/moldelab/modules/export/**`.

## Requisitos inegociáveis
- Escala 1:1 exata (mm × 72/25,4 pt). Quadrado de teste de 5 cm na primeira folha do molde e instrução "imprimir a 100%".
- Folhas numeradas (linha = letra, coluna = número), losangos de alinhamento nas bordas, mapa de montagem na capa, folhas vazias omitidas.
- Texto só com caracteres WinAnsi (`safe`), português correto.
- Linhas: corte contínua preta, costura tracejada cinza, dobra traço-ponto azul (por cima do corte), pences/internas traço-ponto, piques e fio com setas.

## Verificação
Testes `PdfPatternRendererTest` (tamanho de página, texto, contagem) **e** inspeção visual: renderize capa e uma folha em PNG com `PDFRenderer` e confira. `./mvnw verify` verde.
