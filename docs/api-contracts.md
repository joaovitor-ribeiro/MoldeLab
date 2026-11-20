# Contrato da API (`/api`)

Unidades: medidas e parâmetros em **cm**; geometria em **mm** (pontos `[x, y]`, y para baixo, 2 casas).
Erros: `{ "code": string, "message": string, "details": object }`.

| Código | HTTP | Quando |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Bean Validation (`details.fields`) |
| `MALFORMED_JSON` / `INVALID_ARGUMENT` | 400 | corpo ilegível / argumento inválido |
| `NOT_FOUND` | 404 | recurso inexistente |
| `INTEGRITY_VIOLATION` | 409 | referência inválida (ex.: `profileId` inexistente) |
| `INVALID_MEASUREMENTS` | 422 | faltam medidas do modelo ou fora do plausível (`details.missing`, `details.outOfRange` com nomes JSON) |
| `FILE_TOO_LARGE` | 413 | imagem > 8 MB |
| `UNSUPPORTED_IMAGE` | 415 | não é PNG/JPEG/WEBP/GIF (detectado pelos bytes) |
| `AI_FAILED` | 502 | falha/recusa da IA |
| `AI_DISABLED` | 503 | sem `ANTHROPIC_API_KEY` |
| `INTERNAL_ERROR` | 500 | inesperado (sem stack trace) |

## Medidas
`Measurements` (todas opcionais, positivas): `height, bust, waist, hip, neck, shoulderWidth, backWaistLength, armLength, upperArm, wrist, waistToHip, waistToKnee, waistToFloor, thigh, crotchDepth`.

- `GET /measurements/guide` → `[{ key, jsonName, label, howToMeasure, minCm, maxCm }]`
- `GET /measurements/standard-sizes` → `[{ id, label, group, measurements }]`
- `GET /profiles` → `Profile[]` (mais recente primeiro)
- `GET /profiles/{id}` → `Profile` | 404
- `POST /profiles` `{ name (1–120), notes? (≤2000), measurements }` → 201 `Profile`
- `PUT /profiles/{id}` (mesmo corpo; substitui as medidas) → `Profile`
- `DELETE /profiles/{id}` → 204 (projetos que apontavam ficam com `profileId = null`)

`Profile = { id, name, notes?, measurements, createdAt, updatedAt }`

## Catálogo e moldes
- `GET /catalog` → `[{ type, label, description, requiredMeasurements: [{key, jsonName, label}], params: ParamSpec[] }]`
  - `ParamSpec = { key, label, unit, min, max, defaultValue, step, help, options: [{value, label}] }`
  - `type ∈ SAIA_RETA | SAIA_GODE | CAMISETA`
- `POST /patterns/draft`
  ```json
  { "garmentType": "CAMISETA", "measurements": {...}, "params": {"sleeveLength": 25},
    "seams": {"seamCm": 1, "hemCm": 2.5, "finishCm": 0.7}, "fabricWidthCm": 150 }
  ```
  → `Pattern = { garmentType, garmentLabel, params (valores usados), seams (mm), measurements, pieces[], fabric[], warnings[] }`
  - `piece = { id, code, name, material, cutQuantity, cutOnFold, mirrored, seamLine, cutLine, foldLines, grainline{from,to}, notches[{from,to}], internalLines, edges[{name, kind, lengthMm}], labelAnchor, bounds{minX,minY,maxX,maxY} }`
  - `fabric = [{ material, fabricWidthMm, lengthMm }]`
  - Parâmetros fora do limite são ajustados e geram aviso; chaves desconhecidas são ignoradas com aviso.
- `POST /patterns/pdf` `{ draft: DraftRequest, paper?: A4|LETTER|A3|A0|PLOTTER_914|PLOTTER_1067 (padrão A4), title? (≤80), profileName?, showSeamLine? (padrão true) }` → `application/pdf` com `Content-Disposition: attachment; filename="<slug>.pdf"`

## IA
- `GET /ai/status` → `{ enabled, model? }`
- `POST /ai/analyze-garment` multipart: `image` (≤8 MB), `hint?` (≤500) → `GarmentSuggestion = { garmentType?, supported, confidence (0–1), description?, params, notes[] }`

## Projetos
- `GET /projects` → `[{ id, name, garmentType, hasSketch, updatedAt }]`
- `GET /projects/{id}` → `{ id, name, garmentType, profileId?, measurements, params, seams, sketchDataUrl?, createdAt, updatedAt }`
- `POST /projects` `{ name, garmentType, profileId?, measurements, params?, seams?, sketchDataUrl? (data:image/png|jpeg|webp;base64, ≤6 MB) }` → 201
- `PUT /projects/{id}` (mesmo corpo) · `DELETE /projects/{id}` → 204

## Saúde
- `GET /actuator/health` → `{ "status": "UP" }`
