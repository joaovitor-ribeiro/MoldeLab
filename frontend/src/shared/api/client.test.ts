import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiSend, ApiError } from './client'

afterEach(() => vi.unstubAllGlobals())

describe('api client', () => {
  it('converte erro da API em ApiError com código e detalhes', async () => {
    vi.stubGlobal('fetch', vi.fn(async () =>
      new Response(JSON.stringify({ code: 'INVALID_MEASUREMENTS', message: 'Faltam medidas: Quadril', details: { missing: ['hip'] } }), {
        status: 422,
        headers: { 'Content-Type': 'application/json' },
      }),
    ))
    const err = await apiSend('POST', '/patterns/draft', {}).catch((e: unknown) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect(err).toMatchObject({ status: 422, code: 'INVALID_MEASUREMENTS', details: { missing: ['hip'] } })
  })

  it('falha de rede vira mensagem amigável', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch') }))
    await expect(apiSend('POST', '/x', {})).rejects.toMatchObject({ code: 'NETWORK' })
  })

  it('204 devolve undefined', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 204 })))
    await expect(apiSend('DELETE', '/profiles/1')).resolves.toBeUndefined()
  })

  it('resposta sem JSON (ex.: 403 do proxy) nunca vira mensagem vazia', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('Invalid CORS request', { status: 403, statusText: '' })))
    const err = (await apiSend('POST', '/ai/analyze-garment', {}).catch((e: unknown) => e)) as ApiError
    expect(err.status).toBe(403)
    expect(err.message).toMatch(/403/)
  })
})
