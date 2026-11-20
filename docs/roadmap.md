# Roadmap

## v0.1 — MVP (feito)
Perfis de medidas editáveis · 3 modelos (saia reta/evasê, godê, camiseta/vestido) · prévia ao vivo · PDF A4/A3/A0/plotter · leitura de desenho/foto por IA · provador 3D paramétrico · projetos salvos.

## v0.2 — Qualidade do molde
- [ ] Prova física de cada modelo em 3 tamanhos; calibrar folgas, cava e cabeça de manga (registrar em `docs/modelagem/regras.md`).
- [ ] Blusa base de tecido plano com pences de busto; vestido tubinho (blusa + saia).
- [ ] Calça básica (frente/costas com gancho) — medidas já existem (`crotchDepth`, `thigh`, `waistToFloor`).
- [ ] Instruções de costura passo a passo na capa do PDF (ordem de montagem por modelo).
- [ ] Exportar SVG/DXF.

## v0.3 — Medidas e prova
- [ ] **Medidas por foto**: fotos frente/perfil + altura → MediaPipe Pose (no navegador) estima larguras; profundidade por proporção → circunferências; usuário revisa e edita.
- [ ] **Ajustes de prova**: formulário "ficou largo/apertado em X" → correção automática dos parâmetros/medidas.
- [ ] Provador 3D com mais detalhe: decote recortado, manga seguindo a cava, cós.

## v0.4 — Prova virtual com foto
- [ ] Try-on 2D: foto da pessoa + roupa → imagem gerada vestindo a roupa (serviço de IA generativa de imagem; avaliar custo/privacidade; consentimento explícito).
- [ ] Sobrepor o manequim 3D à foto (pose estimada) para comparar proporções.

## v0.5 — Ateliê
- [ ] Gradação (vários tamanhos sobrepostos no mesmo PDF).
- [ ] Encaixe otimizado no tecido (nesting com rotação 0/180°, respeitando fio) e plano de corte.
- [ ] Contas de usuário e PostgreSQL (migração: trocar URL, `CLOB`→`TEXT`).
- [ ] Biblioteca de modelos da comunidade (drafters parametrizados declarativos).
