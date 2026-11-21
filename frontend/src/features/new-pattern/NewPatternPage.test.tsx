import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import NewPatternPage from './NewPatternPage'

const catalog = [
  { type: 'SAIA_RETA', label: 'Saia reta / evasê', description: 'Saia com pences', requiredMeasurements: [], params: [] },
  { type: 'CAMISETA', label: 'Camiseta', description: 'Malha', requiredMeasurements: [], params: [] },
]

function renderPage(aiEnabled: boolean) {
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    const body = url.endsWith('/ai/status') ? { enabled: aiEnabled, model: 'x' } : catalog
    return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
  }))
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <MemoryRouter><NewPatternPage /></MemoryRouter>
    </QueryClientProvider>,
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('Novo molde — usabilidade', () => {
  it('mostra os passos e deixa claro que a IA é opcional', async () => {
    renderPage(true)
    expect(screen.getByRole('heading', { name: 'Imagem da roupa' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Escolha o modelo' })).toBeInTheDocument()
    expect(await screen.findByText('opcional')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: /Saia reta/ })).toBeInTheDocument()
  })

  it('IA fica desabilitada até existir uma imagem, com explicação', async () => {
    renderPage(true)
    const btn = await screen.findByRole('button', { name: 'Analisar imagem com IA' })
    expect(btn).toBeDisabled()
    expect(screen.getByText(/Envie uma foto no passo 1/)).toBeInTheDocument()
  })

  it('sem imagem: esconde a IA e mostra só a escolha do modelo', async () => {
    renderPage(true)
    await userEvent.click(screen.getByRole('tab', { name: 'Sem imagem' }))
    expect(screen.queryByRole('button', { name: 'Analisar imagem com IA' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Qual modelo você quer?' })).toBeInTheDocument()
  })

  it('IA desligada no servidor: explica e mantém a escolha manual', async () => {
    renderPage(false)
    expect(await screen.findByText(/não está configurada/)).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: /Camiseta/ })).toBeEnabled()
  })
})
