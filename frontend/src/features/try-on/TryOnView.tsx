import { ContactShadows, OrbitControls } from '@react-three/drei'
import { Canvas } from '@react-three/fiber'
import { useMemo, useState } from 'react'
import * as THREE from 'three'
import type { GarmentType, Measurements } from '@/shared/api/types'
import { readFileAsDataUrl } from '@/shared/lib/download'
import { editorStore, useEditorDraft } from '@/shared/state/editorStore'
import { Label } from '@/shared/ui/ui'
import { buildBody, buildGarment, DEPTH, type BodyModel, type GarmentShape } from './bodyModel'

const SKIN_TONES = ['#e9cdb5', '#d4a98a', '#b07d5a', '#7c5236', '#4b3022']

export default function TryOnView({
  garmentType,
  params,
  measurements,
}: {
  garmentType: GarmentType
  params: Record<string, number>
  measurements: Measurements
}) {
  const { fabricColor } = useEditorDraft()
  const [skin, setSkin] = useState(SKIN_TONES[1])
  const [photo, setPhoto] = useState<string | null>(null)
  const body = useMemo(() => buildBody(measurements), [measurements])
  const garment = useMemo(() => buildGarment(garmentType, params, body), [garmentType, params, body])

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_240px]">
      <div className="relative h-[70vh] min-h-[420px] overflow-hidden rounded-lg border border-line bg-gradient-to-b from-paper to-paper-2">
        <Canvas camera={{ position: [0, body.height * 0.52, 3.3], fov: 35 }} dpr={[1, 2]} shadows aria-label="Provador 3D">
          <ambientLight intensity={0.55} />
          <directionalLight position={[2, 3, 2]} intensity={1.2} castShadow />
          <directionalLight position={[-2, 2, -1.5]} intensity={0.4} />
          <Mannequin body={body} skin={skin} />
          <Garment shape={garment} body={body} color={fabricColor} />
          <ContactShadows position={[0, 0, 0]} opacity={0.35} scale={3} blur={2.2} far={2} />
          <OrbitControls target={[0, body.height * 0.5, 0]} enablePan={false} minDistance={1.2} maxDistance={4.5} />
        </Canvas>
        <p className="pointer-events-none absolute bottom-2 left-3 text-xs text-muted">Arraste para girar · role para aproximar</p>
      </div>

      <div className="space-y-5 text-sm">
        <div>
          <Label htmlFor="fabric-color">Cor do tecido</Label>
          <input
            id="fabric-color"
            type="color"
            value={fabricColor}
            onChange={(e) => editorStore.set({ fabricColor: e.target.value })}
            className="h-10 w-full cursor-pointer rounded-lg border border-line bg-white"
          />
        </div>
        <fieldset>
          <legend className="mb-1 text-sm font-medium text-ink-2">Tom do manequim</legend>
          <div className="flex gap-2">
            {SKIN_TONES.map((t) => (
              <button
                key={t}
                type="button"
                aria-label={`Tom ${t}`}
                aria-pressed={skin === t}
                onClick={() => setSkin(t)}
                className="size-8 rounded-full border-2 border-transparent aria-pressed:border-ink"
                style={{ background: t }}
              />
            ))}
          </div>
        </fieldset>
        <div>
          <Label htmlFor="own-photo">Sua foto (comparação)</Label>
          <input
            id="own-photo"
            type="file"
            accept="image/*"
            className="block w-full text-xs"
            onChange={async (e) => {
              const f = e.target.files?.[0]
              if (f) setPhoto(await readFileAsDataUrl(f))
            }}
          />
          {photo && <img src={photo} alt="Sua foto para comparar proporções" className="mt-2 max-h-72 w-full rounded-lg object-contain" />}
          <p className="mt-2 text-xs text-muted">
            A foto fica só no seu navegador. O manequim usa as suas medidas; vestir a roupa sobre a própria foto está no roadmap.
          </p>
        </div>
        <p className="rounded-lg bg-paper-2 p-3 text-xs text-ink-2">
          Visualização aproximada de proporções (comprimento, folga, evasê). O caimento real depende do tecido.
        </p>
      </div>
    </div>
  )
}

function Mannequin({ body, skin }: { body: BodyModel; skin: string }) {
  const torsoGeo = useMemo(
    () => new THREE.LatheGeometry(body.torso.map((r) => new THREE.Vector2(r.r, r.y)), 48),
    [body],
  )
  const material = <meshStandardMaterial color={skin} roughness={0.75} />
  const { leg, arm } = body
  const thighLen = body.crotch - body.knee
  const shinLen = body.knee - 0.06
  return (
    <group>
      <mesh geometry={torsoGeo} scale={[1, 1, DEPTH]} castShadow>{material}</mesh>
      {/* pescoço e cabeça */}
      <mesh position={[0, body.neck + 0.04, 0]} castShadow>
        <cylinderGeometry args={[body.torso[10].r * 0.9, body.torso[10].r, 0.09, 24]} />
        {material}
      </mesh>
      <mesh position={[0, body.neck + 0.08 + body.headRadius, 0.01]} scale={[0.86, 1.08, 0.95]} castShadow>
        <sphereGeometry args={[body.headRadius, 32, 24]} />
        {material}
      </mesh>
      {/* pernas */}
      {[-1, 1].map((s) => (
        <group key={s} position={[s * leg.x, 0, 0]}>
          <mesh position={[0, body.knee + thighLen / 2, 0]} castShadow>
            <cylinderGeometry args={[leg.topR, leg.kneeR, thighLen, 24]} />
            {material}
          </mesh>
          <mesh position={[0, 0.06 + shinLen / 2, 0]} castShadow>
            <cylinderGeometry args={[leg.kneeR, leg.ankleR, shinLen, 24]} />
            {material}
          </mesh>
          <mesh position={[0, 0.03, 0.04]} castShadow>
            <boxGeometry args={[leg.ankleR * 2.2, 0.06, 0.22]} />
            {material}
          </mesh>
        </group>
      ))}
      {/* braços */}
      {[-1, 1].map((s) => (
        <group key={s} position={[s * arm.x, arm.y, 0]} rotation={[0, 0, s * arm.angle]}>
          <mesh position={[0, -arm.length / 2, 0]} castShadow>
            <cylinderGeometry args={[arm.topR, arm.wristR, arm.length, 20]} />
            {material}
          </mesh>
          <mesh position={[0, -arm.length - 0.06, 0]} scale={[0.7, 1.3, 0.4]}>
            <sphereGeometry args={[0.045, 16, 12]} />
            {material}
          </mesh>
        </group>
      ))}
    </group>
  )
}

function Garment({ shape, body, color }: { shape: GarmentShape; body: BodyModel; color: string }) {
  const geometry = useMemo(() => ringGeometry(shape), [shape])
  const mat = <meshStandardMaterial color={color} roughness={0.85} side={THREE.DoubleSide} />
  const { arm } = body
  return (
    <group>
      <mesh geometry={geometry} castShadow>{mat}</mesh>
      {shape.sleeves &&
        [-1, 1].map((s) => (
          <group key={s} position={[s * arm.x, arm.y + 0.01, 0]} rotation={[0, 0, s * arm.angle]}>
            <mesh position={[0, -shape.sleeves!.length / 2 + 0.005, 0]} castShadow>
              <cylinderGeometry args={[shape.sleeves!.topR + 0.008, shape.sleeves!.bottomR + 0.006, shape.sleeves!.length, 24, 1, true]} />
              {mat}
            </mesh>
          </group>
        ))}
    </group>
  )
}

/** Tubo por anéis elípticos, com ondulação (drapeado) crescendo em direção à barra. */
function ringGeometry(shape: GarmentShape): THREE.BufferGeometry {
  const seg = 72
  const rings = shape.rings
  const top = rings[0].y
  const bottom = rings[rings.length - 1].y
  const positions: number[] = []
  const indices: number[] = []
  for (let i = 0; i < rings.length; i++) {
    const { y, r } = rings[i]
    const t = top === bottom ? 0 : (top - y) / (top - bottom)
    for (let j = 0; j <= seg; j++) {
      const a = (j / seg) * Math.PI * 2
      const wave = 1 + shape.foldAmp * Math.pow(t, 1.4) * Math.sin(shape.folds * a)
      positions.push(Math.cos(a) * r * wave, y, Math.sin(a) * r * DEPTH * wave * (shape.folds ? 1.1 : 1))
    }
  }
  for (let i = 0; i < rings.length - 1; i++) {
    for (let j = 0; j < seg; j++) {
      const a = i * (seg + 1) + j
      const b = a + seg + 1
      indices.push(a, b, a + 1, b, b + 1, a + 1)
    }
  }
  const g = new THREE.BufferGeometry()
  g.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3))
  g.setIndex(indices)
  g.computeVertexNormals()
  return g
}
