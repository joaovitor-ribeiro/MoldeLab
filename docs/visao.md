# Visão do produto — MoldeLab

## Problema
Quem costura (por hobby ou profissão) encontra uma roupa que quer fazer — numa foto, numa vitrine, na cabeça — mas transformar isso num **molde nas próprias medidas** exige conhecimento de modelagem plana, régua de alfaiate e horas de traçado. Moldes prontos vêm em tamanhos padrão que raramente vestem bem.

## Proposta
1. A pessoa **desenha** a roupa (prancheta no app) ou **envia uma foto**.
2. A IA identifica o **modelo base** mais próximo e estima os parâmetros de estilo (comprimento, rodado, manga, decote...).
3. A pessoa cadastra/edita suas **medidas** (ou parte de um tamanho padrão).
4. O motor de modelagem gera as **peças** com margens de costura, piques, fio, dobra e pences.
5. Um **provador 3D** mostra proporções da roupa num manequim com as medidas da pessoa.
6. Ela baixa um **PDF em tamanho real**: imprime em A4 (folhas numeradas com mapa de montagem e quadrado de teste) ou manda para plotter, monta, recorta e costura (ou entrega para uma costureira).

## Público
- Costureiras(os) amadoras(es) que querem roupas sob medida.
- Pequenos ateliês que precisam de moldes base rápidos por cliente (perfis de medidas por cliente).

## Escopo do MVP (v0.1 — implementado)
- Perfis de medidas (CRUD, edição completa, tabelas padrão feminino 36–48 e masculino P–XG, guia "como medir").
- Modelos: **saia reta/evasê** (com pences e cós), **saia godê** (1/4, 1/2, 3/4, inteiro), **camiseta/blusa/vestido-camiseta** (frente, costas, manga com cabeça calculada pela cava, ribana).
- Editor com prévia ao vivo, parâmetros, margens por tipo de borda, estimativa de tecido, avisos.
- PDF: capa com instruções, quadrado de teste 5 cm, mapa de montagem, lista de peças, consumo de tecido; páginas A4/Carta/A3/A0 com losangos de alinhamento e folhas vazias omitidas; plotter 91,4/106,7 cm.
- Leitura de desenho/foto por IA (Claude) com fallback manual.
- Provador 3D paramétrico (manequim pelas medidas + roupa pelos parâmetros, cor do tecido, tom do manequim).
- Projetos salvos (modelo + parâmetros + medidas + imagem de referência).

## Fora do escopo do MVP
- Simulação física de tecido; vestir a roupa sobre a foto da pessoa (ver roadmap).
- Modelos com gola, abotoamento, calça, macacão.
- Contas de usuário / nuvem (app local, um usuário).

## Sugestões (além do pedido original)
Priorizadas em `docs/roadmap.md`. Destaques:
- **Medidas a partir de foto**: duas fotos (frente/lado) + altura → estimativa das circunferências por visão computacional (MediaPipe Pose no navegador), sempre editável.
- **Gradação**: gerar o mesmo molde em vários tamanhos sobrepostos (útil para ateliês).
- **Encaixe otimizado** no tecido (nesting) com exportação para corte e economia de tecido.
- **Instruções de montagem** passo a passo geradas por peça/modelo (ordem de costura).
- **Prova virtual com IA generativa**: imagem da pessoa vestindo a roupa a partir da foto dela (try-on 2D), como complemento ao 3D paramétrico.
- **Ajustes de prova**: "sobrou na cintura", "apertou no quadril" → correções automáticas no molde.
- Exportar **SVG/DXF** para plotter de recorte e softwares CAD de moda.
