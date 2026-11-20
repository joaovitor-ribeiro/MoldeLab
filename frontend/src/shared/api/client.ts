export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: Record<string, unknown>

  constructor(status: number, code: string, message: string, details: Record<string, unknown> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }
}

const BASE = '/api'

async function toError(res: Response): Promise<ApiError> {
  try {
    const body = (await res.json()) as { code?: string; message?: string; details?: Record<string, unknown> }
    return new ApiError(res.status, body.code ?? 'HTTP_' + res.status, body.message ?? res.statusText, body.details ?? {})
  } catch {
    return new ApiError(
      res.status,
      'HTTP_' + res.status,
      res.status >= 500 || res.status === 0 ? 'Servidor indisponível. O backend está rodando?' : res.statusText,
    )
  }
}

async function request(path: string, init?: RequestInit): Promise<Response> {
  let res: Response
  try {
    res = await fetch(BASE + path, init)
  } catch {
    throw new ApiError(0, 'NETWORK', 'Não foi possível falar com o servidor. O backend está rodando na porta 8080?')
  }
  if (!res.ok) throw await toError(res)
  return res
}

export async function apiGet<T>(path: string, signal?: AbortSignal): Promise<T> {
  return (await request(path, { signal })).json() as Promise<T>
}

export async function apiSend<T>(method: 'POST' | 'PUT' | 'DELETE', path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  const res = await request(path, {
    method,
    signal,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export async function apiBlob(path: string, body: unknown): Promise<{ blob: Blob; filename: string | null }> {
  const res = await request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const cd = res.headers.get('Content-Disposition') ?? ''
  const star = /filename\*=UTF-8''([^;]+)/i.exec(cd)
  const plain = /filename="?([^";]+)"?/i.exec(cd)
  const filename = star ? decodeURIComponent(star[1]) : plain ? plain[1] : null
  return { blob: await res.blob(), filename }
}

export async function apiUpload<T>(path: string, form: FormData): Promise<T> {
  return (await request(path, { method: 'POST', body: form })).json() as Promise<T>
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) return e.message
  if (e instanceof Error) return e.message
  return 'Erro inesperado'
}
