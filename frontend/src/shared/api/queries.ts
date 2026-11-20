import { queryOptions } from '@tanstack/react-query'
import { apiGet } from './client'
import type { AiStatus, CatalogItem, MeasurementGuide, Profile, Project, ProjectSummary, StandardSize } from './types'

// Consultas compartilhadas entre features (dados de referência e entidades salvas).

export const catalogQuery = queryOptions({
  queryKey: ['catalog'],
  queryFn: ({ signal }) => apiGet<CatalogItem[]>('/catalog', signal),
  staleTime: Infinity,
})

export const guideQuery = queryOptions({
  queryKey: ['measurements', 'guide'],
  queryFn: ({ signal }) => apiGet<MeasurementGuide[]>('/measurements/guide', signal),
  staleTime: Infinity,
})

export const standardSizesQuery = queryOptions({
  queryKey: ['measurements', 'standard-sizes'],
  queryFn: ({ signal }) => apiGet<StandardSize[]>('/measurements/standard-sizes', signal),
  staleTime: Infinity,
})

export const profilesQuery = queryOptions({
  queryKey: ['profiles'],
  queryFn: ({ signal }) => apiGet<Profile[]>('/profiles', signal),
})

export const projectsQuery = queryOptions({
  queryKey: ['projects'],
  queryFn: ({ signal }) => apiGet<ProjectSummary[]>('/projects', signal),
})

export const projectQuery = (id: string) =>
  queryOptions({
    queryKey: ['projects', id],
    queryFn: ({ signal }) => apiGet<Project>('/projects/' + id, signal),
  })

export const aiStatusQuery = queryOptions({
  queryKey: ['ai', 'status'],
  queryFn: ({ signal }) => apiGet<AiStatus>('/ai/status', signal),
  staleTime: 60_000,
})
