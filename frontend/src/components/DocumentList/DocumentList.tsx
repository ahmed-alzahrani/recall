import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAllDocuments } from '../../services/api'
import type { Document } from '../../types/document'
import './DocumentList.css'


function DocumentList() {
  const [documents, setDocuments] = useState<Document[]>([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    const fetchDocuments = async () => {
      try {
        const data = await getAllDocuments()
        setDocuments(data)
      } catch (error) {
        console.error('Failed to fetch documents:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchDocuments()
  }, [])

  const handleDocumentClick = (documentId: number, status: string) => {
    if (status === 'COMPLETED') {
      navigate(`/chat/${documentId}`)
    }
  }

  if (loading) {
    return <div className="document-list">Loading documents...</div>
  }

  return (
    <div className="document-list">
      <h3>Documents</h3>
      {documents.length === 0 ? (
        <p>No documents yet</p>
      ) : (
        <div className="document-items">
          {documents.map((doc) => (
            <div
              key={doc.documentId}
              className={`document-item ${doc.status !== 'COMPLETED' ? 'disabled' : ''}`}
              onClick={() => handleDocumentClick(doc.documentId, doc.status)}
            >
              <div className="document-filename">{doc.filename}</div>
              <div className="document-status">Status: {doc.status}</div>
              {doc.summary && (
                <div className="document-summary">{doc.summary}</div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default DocumentList