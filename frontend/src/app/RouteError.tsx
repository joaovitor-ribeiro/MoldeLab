import { isRouteErrorResponse, Link, useRouteError } from 'react-router-dom'
import { editorStore } from '@/shared/state/editorStore'
import { Button, Card } from '@/shared/ui/ui'

/** Tela de erro das rotas: mantém o cabeçalho e oferece saídas em vez da tela crua do React Router. */
export function RouteError({ notFound: forcedNotFound = false }: { notFound?: boolean } = {}) {
  const error = useRouteError()
  const notFound = forcedNotFound || (isRouteErrorResponse(error) && error.status === 404)
  const detail = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : error instanceof Error
      ? error.message
      : String(error)

  if (!notFound) console.error(error)

  return (
    <div className="mx-auto max-w-xl p-6 sm:p-10">
      <Card className="p-6">
        <h1 className="font-display text-2xl">{notFound ? 'Página não encontrada' : 'Algo deu errado nesta tela'}</h1>
        <p className="mt-2 text-sm text-muted">
          {notFound
            ? 'O endereço não existe no MoldeLab.'
            : 'Suas medidas e projetos salvos não foram afetados. Tente recarregar; se o erro voltar, recomece o editor.'}
        </p>
        {!notFound && (
          <pre className="mt-4 overflow-x-auto rounded-lg bg-paper-2 p-3 text-xs text-ink-2">{detail}</pre>
        )}
        <div className="mt-5 flex flex-wrap gap-2">
          {!notFound && <Button onClick={() => window.location.reload()}>Recarregar</Button>}
          {!notFound && (
            <Button
              variant="secondary"
              onClick={() => {
                editorStore.reset()
                window.location.assign('/editor')
              }}
            >
              Recomeçar o editor
            </Button>
          )}
          <Link to="/" className="inline-flex items-center rounded-lg px-4 py-2 text-sm font-medium text-ink-2 hover:bg-paper-2">
            Ir para o início
          </Link>
        </div>
      </Card>
    </div>
  )
}
