import { useEffect, useRef, useState, type FormEvent } from 'react'
import { getErrorMessage } from '../lib/apiError'
import { listDocuments, processDocument, uploadDocument } from '../lib/documentApi'
import type { KnowledgeDocument, PagedResponse } from '../types/document'

const PAGE_SIZE = 10
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(new Date(value))
}

function canProcess(document: KnowledgeDocument): boolean {
  return document.status === 'UPLOADED' || document.status === 'FAILED'
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

export default function DocumentsPage() {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [page, setPage] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [documentPage, setDocumentPage] = useState<PagedResponse<KnowledgeDocument> | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [displayName, setDisplayName] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [processingId, setProcessingId] = useState<string | null>(null)
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    listDocuments(page, PAGE_SIZE, controller.signal)
      .then(setDocumentPage)
      .catch((error: unknown) => {
        if (!isAbortError(error)) {
          setLoadError(getErrorMessage(error, 'Unable to load knowledge documents.'))
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => controller.abort()
  }, [page, reloadVersion])

  function reloadDocuments() {
    setLoading(true)
    setLoadError(null)
    setReloadVersion((current) => current + 1)
  }

  function changePage(nextPage: number) {
    setLoading(true)
    setLoadError(null)
    setPage(nextPage)
  }

  function handleFileSelection(file: File | null) {
    setFormError(null)
    setSelectedFile(null)

    if (file === null) return
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      setFormError('Select a file with a .pdf extension.')
      return
    }
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setFormError('The PDF must be 10 MB or smaller.')
      return
    }

    setSelectedFile(file)
    if (displayName.trim() === '') {
      setDisplayName(file.name.replace(/\.pdf$/i, ''))
    }
  }

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (displayName.trim() === '') {
      setFormError('Display name is required.')
      return
    }
    if (selectedFile === null) {
      setFormError('Choose a PDF to upload.')
      return
    }

    setUploading(true)
    setFormError(null)
    setActionError(null)
    setActionMessage(null)

    try {
      const uploadedDocument = await uploadDocument(displayName.trim(), selectedFile)
      setDisplayName('')
      setSelectedFile(null)
      if (fileInputRef.current !== null) fileInputRef.current.value = ''
      setActionMessage(`${uploadedDocument.displayName} was uploaded and is ready to process.`)
      if (page === 0) {
        reloadDocuments()
      } else {
        changePage(0)
      }
    } catch (error) {
      setFormError(getErrorMessage(error, 'Unable to upload the PDF.'))
    } finally {
      setUploading(false)
    }
  }

  async function handleProcess(document: KnowledgeDocument) {
    setProcessingId(document.id)
    setActionError(null)
    setActionMessage(null)

    try {
      const processedDocument = await processDocument(document.id)
      setDocumentPage((current) => current === null ? current : {
        ...current,
        content: current.content.map((item) =>
          item.id === processedDocument.id ? processedDocument : item,
        ),
      })
      setActionMessage(
        `${processedDocument.displayName} is ready with ${processedDocument.pageCount ?? 0} pages.`,
      )
    } catch (error) {
      setActionError(getErrorMessage(error, 'Document processing failed.'))
      reloadDocuments()
    } finally {
      setProcessingId(null)
    }
  }

  return (
    <section className="workspace-main document-workspace">
      <div className="document-heading">
        <div>
          <p className="eyebrow"><span aria-hidden="true" />Administrator knowledge base</p>
          <h1>Documents become evidence.</h1>
          <p>
            Upload trusted PDFs, then process them into page-aware chunks and vector embeddings
            before employees can retrieve them.
          </p>
        </div>
        <span className="document-count">
          <strong>{documentPage?.totalElements ?? 0}</strong>
          <small>Total documents</small>
        </span>
      </div>

      <div className="document-layout">
        <aside className="upload-panel">
          <p className="card-kicker">Add knowledge</p>
          <h2>Upload a PDF</h2>
          <p>Files are registered first and processed separately so failures can be retried.</p>

          <form className="upload-form" onSubmit={handleUpload} noValidate>
            {formError !== null && <div className="form-alert" role="alert">{formError}</div>}

            <div className="form-field">
              <label htmlFor="document-display-name">Display name</label>
              <input
                id="document-display-name"
                maxLength={255}
                value={displayName}
                placeholder="Employee Handbook"
                onChange={(event) => setDisplayName(event.target.value)}
              />
            </div>

            <div className="form-field">
              <label htmlFor="document-file">PDF file</label>
              <input
                ref={fileInputRef}
                id="document-file"
                className="file-input"
                type="file"
                accept="application/pdf,.pdf"
                onChange={(event) => handleFileSelection(event.target.files?.[0] ?? null)}
              />
              <span className="field-hint">
                {selectedFile === null
                  ? 'Maximum size: 10 MB'
                  : `${selectedFile.name} · ${formatFileSize(selectedFile.size)}`}
              </span>
            </div>

            <button className="submit-button" type="submit" disabled={uploading}>
              {uploading ? 'Uploading…' : 'Upload document'}
            </button>
          </form>
        </aside>

        <div className="document-list-panel">
          <div className="list-heading">
            <div>
              <p className="card-kicker">Document lifecycle</p>
              <h2>Knowledge documents</h2>
            </div>
            <button
              className="secondary-button"
              type="button"
              onClick={reloadDocuments}
              disabled={loading}
            >
              Refresh
            </button>
          </div>

          {actionMessage !== null && <div className="success-alert" role="status">{actionMessage}</div>}
          {actionError !== null && <div className="form-alert" role="alert">{actionError}</div>}

          {loading && <div className="document-state" aria-live="polite">Loading documents…</div>}

          {!loading && loadError !== null && (
            <div className="document-state error-state">
              <p>{loadError}</p>
              <button className="secondary-button" type="button" onClick={reloadDocuments}>
                Try again
              </button>
            </div>
          )}

          {!loading && loadError === null && documentPage?.content.length === 0 && (
            <div className="document-state empty-state">
              <span aria-hidden="true">PDF</span>
              <h3>No documents yet.</h3>
              <p>Upload the first trusted source to start building the knowledge base.</p>
            </div>
          )}

          {!loading && loadError === null && documentPage !== null && documentPage.content.length > 0 && (
            <div className="document-list">
              {documentPage.content.map((document) => {
                const processing = processingId === document.id
                return (
                  <article className="document-row" key={document.id}>
                    <div className="document-file-icon" aria-hidden="true">PDF</div>
                    <div className="document-primary">
                      <div className="document-title-row">
                        <h3>{document.displayName}</h3>
                        <span className={`status-badge status-${document.status.toLowerCase()}`}>
                          {processing ? 'PROCESSING' : document.status}
                        </span>
                      </div>
                      <p>{document.originalFileName}</p>
                      <div className="document-metadata">
                        <span>{formatFileSize(document.sizeBytes)}</span>
                        <span>{document.pageCount === null ? 'Pages pending' : `${document.pageCount} pages`}</span>
                        <span>Added {formatDate(document.createdAt)}</span>
                      </div>
                      {document.failureReason !== null && (
                        <p className="failure-reason">{document.failureReason}</p>
                      )}
                    </div>
                    <div className="document-action">
                      {canProcess(document) ? (
                        <button
                          className="secondary-button"
                          type="button"
                          disabled={processingId !== null}
                          onClick={() => void handleProcess(document)}
                        >
                          {processing ? 'Processing…' : document.status === 'FAILED' ? 'Retry' : 'Process'}
                        </button>
                      ) : (
                        <span className="ready-label">
                          {document.status === 'READY' ? 'Ready for questions' : 'Processing'}
                        </span>
                      )}
                    </div>
                  </article>
                )
              })}
            </div>
          )}

          {!loading && loadError === null && documentPage !== null && documentPage.totalPages > 1 && (
            <nav className="pagination" aria-label="Document pages">
              <button
                className="secondary-button"
                type="button"
                disabled={documentPage.first || loading}
                onClick={() => changePage(Math.max(0, page - 1))}
              >
                Previous
              </button>
              <span>Page {documentPage.page + 1} of {documentPage.totalPages}</span>
              <button
                className="secondary-button"
                type="button"
                disabled={documentPage.last || loading}
                onClick={() => changePage(page + 1)}
              >
                Next
              </button>
            </nav>
          )}
        </div>
      </div>
    </section>
  )
}
