export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED'

export interface KnowledgeDocument {
  id: string
  displayName: string
  originalFileName: string
  contentType: string
  sizeBytes: number
  status: DocumentStatus
  pageCount: number | null
  failureReason: string | null
  uploadedByUserId: string
  createdAt: string
  updatedAt: string
}
