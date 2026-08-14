import { authenticatedApiRequest } from '../auth/authSession'
import type { PagedResponse } from '../types/api'
import type { KnowledgeDocument } from '../types/document'

export function listDocuments(
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<PagedResponse<KnowledgeDocument>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  return authenticatedApiRequest<PagedResponse<KnowledgeDocument>>(
    `/admin/documents?${query}`,
    { signal },
  )
}

export function uploadDocument(
  displayName: string,
  file: File,
): Promise<KnowledgeDocument> {
  const formData = new FormData()
  formData.append('displayName', displayName)
  formData.append('file', file)

  return authenticatedApiRequest<KnowledgeDocument>('/admin/documents', {
    method: 'POST',
    body: formData,
  })
}

export function processDocument(documentId: string): Promise<KnowledgeDocument> {
  return authenticatedApiRequest<KnowledgeDocument>(
    `/admin/documents/${encodeURIComponent(documentId)}/process`,
    { method: 'POST' },
  )
}
