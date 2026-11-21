import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { ApiError } from '@/shared/api/client'
import { Spinner } from '@/shared/ui/ui'
import { Layout } from './Layout'
import { HomePage } from './HomePage'
import { RouteError } from './RouteError'

const MeasurementsPage = lazy(() => import('@/features/measurements/MeasurementsPage'))
const NewPatternPage = lazy(() => import('@/features/new-pattern/NewPatternPage'))
const EditorPage = lazy(() => import('@/features/editor/EditorPage'))
const ProjectsPage = lazy(() => import('@/features/projects/ProjectsPage'))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (count, err) => !(err instanceof ApiError && err.status >= 400 && err.status < 500) && count < 2,
      refetchOnWindowFocus: false,
    },
  },
})

const page = (el: React.ReactNode) => (
  <Suspense fallback={<div className="p-8"><Spinner /></div>}>{el}</Suspense>
)

const router = createBrowserRouter([
  {
    element: <Layout />,
    errorElement: <RouteError />,
    children: [
      {
        // erro numa página aparece dentro do layout (cabeçalho continua navegável)
        errorElement: <RouteError />,
        children: [
          { path: '/', element: <HomePage /> },
          { path: '/medidas', element: page(<MeasurementsPage />) },
          { path: '/novo', element: page(<NewPatternPage />) },
          { path: '/editor', element: page(<EditorPage />) },
          { path: '/projetos', element: page(<ProjectsPage />) },
          { path: '*', element: <RouteError notFound /> },
        ],
      },
    ],
  },
])

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}
