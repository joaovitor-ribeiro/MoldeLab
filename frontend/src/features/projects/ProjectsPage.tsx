import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { apiGet, apiSend, errorMessage } from '@/shared/api/client'
import { catalogQuery, projectsQuery } from '@/shared/api/queries'
import type { Profile, Project } from '@/shared/api/types'
import { DEFAULT_SEAMS, editorStore } from '@/shared/state/editorStore'
import { Badge, Banner, Button, Card, Spinner } from '@/shared/ui/ui'

export default function ProjectsPage() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const projects = useQuery(projectsQuery)
  const catalog = useQuery(catalogQuery)

  const open = useMutation({
    mutationFn: async (id: string) => {
      const p = await apiGet<Project>('/projects/' + id)
      let profileName: string | null = null
      if (p.profileId) {
        profileName = await apiGet<Profile>('/profiles/' + p.profileId).then((x) => x.name, () => null)
      }
      return { p, profileName }
    },
    onSuccess: ({ p, profileName }) => {
      editorStore.reset({
        garmentType: p.garmentType,
        params: p.params,
        measurements: p.measurements,
        profileId: profileName ? p.profileId ?? null : null,
        profileName,
        seams: { ...DEFAULT_SEAMS, ...p.seams },
        sketchDataUrl: p.sketchDataUrl ?? null,
        projectId: p.id,
        projectName: p.name,
      })
      navigate('/editor')
    },
  })

  const remove = useMutation({
    mutationFn: (id: string) => apiSend<void>('DELETE', '/projects/' + id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['projects'] }),
  })

  const label = (t: string) => catalog.data?.find((c) => c.type === t)?.label ?? t

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-semibold">Projetos</h1>
        <Button onClick={() => navigate('/novo')}>Novo molde</Button>
      </div>
      {projects.isPending && <Spinner />}
      {projects.isError && <Banner tone="error">{errorMessage(projects.error)}</Banner>}
      {open.isError && <Banner tone="error">{errorMessage(open.error)}</Banner>}
      {remove.isError && <Banner tone="error">{errorMessage(remove.error)}</Banner>}
      {projects.data?.length === 0 && <p className="text-ink-2">Nenhum projeto salvo ainda. Salve pelo editor de molde.</p>}
      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {projects.data?.map((p) => (
          <li key={p.id}>
            <Card className="flex h-full flex-col p-4">
              <h2 className="text-lg font-semibold">{p.name}</h2>
              <div className="mt-1 flex flex-wrap gap-2">
                <Badge>{label(p.garmentType)}</Badge>
                {p.hasSketch && <Badge>com referência</Badge>}
              </div>
              <p className="mt-2 flex-1 text-xs text-muted">Atualizado em {new Date(p.updatedAt).toLocaleString('pt-BR')}</p>
              <div className="mt-3 flex gap-2">
                <Button className="flex-1" disabled={open.isPending} onClick={() => open.mutate(p.id)}>Abrir</Button>
                <Button
                  variant="danger"
                  disabled={remove.isPending}
                  onClick={() => { if (window.confirm(`Excluir "${p.name}"?`)) remove.mutate(p.id) }}
                >
                  Excluir
                </Button>
              </div>
            </Card>
          </li>
        ))}
      </ul>
    </div>
  )
}
