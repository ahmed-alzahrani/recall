import { useDropzone } from 'react-dropzone'
import { useState, useEffect, useRef } from 'react';
import './FileUpload.css';
import { uploadDocument, getDocumentStatus } from '../../services/api';

type UploadStatus = 'idle' | 'uploading' | 'processing' | 'success' | 'error';

interface FileUploadProps {
    onDocumentProcessed?: () => void
}

function FileUpload({ onDocumentProcessed }: FileUploadProps) {

    const [file, setFile] = useState<File | null>(null);
    const [status, setStatus] = useState<UploadStatus>('idle');
    const [error, setError] = useState<string | null>(null);

    const [documentId, setDocumentId] = useState<number | null>(null);
    const pollingIntervalRef = useRef<number | null>(null);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        accept: {
            'application/pdf': ['.pdf']
        },
        maxFiles: 1,
        maxSize: 65 * 1024 * 1024, // 65MB
        onDrop: async (acceptedFiles, rejectedFiles) => {
            setError(null)

            if (rejectedFiles.length > 0) {
                const rejection = rejectedFiles[0]
                const errorCode = rejection.errors[0]?.code

                if (errorCode === 'file-too-large') {
                    setError('File size exceeds 65MB limit')
                } else if (errorCode === 'file-invalid-type') {
                    setError('Only PDF files are allowed')
                } else if (errorCode === 'too-many-files') {
                    setError('Please upload only one file at a time')
                } else {
                    setError('File rejected. Please try again.')
                }
                setStatus('error')
                return
            }

            if (acceptedFiles.length > 0) {
                setFile(acceptedFiles[0])
                setStatus('uploading')

                try {
                    const result = await uploadDocument(acceptedFiles[0])
                    const id = result.documentId
                    setDocumentId(id)
                    setStatus('processing')
                } catch (error) {
                    setError('Upload failed. Please try again.')
                    setStatus('error')
                }
            }
        }
    })

    useEffect(() => {
        if (status === 'processing' && documentId !== null) {
            const pollStatus = async () => {
                try {
                    const statusData = await getDocumentStatus(documentId)
                    // statusData.status will be 'PENDING', 'PROCESSING', 'COMPLETED', or 'FAILED'

                    if (statusData.status === 'COMPLETED' || statusData.status === 'FAILED') {
                        if (pollingIntervalRef.current) {
                            clearInterval(pollingIntervalRef.current)
                            pollingIntervalRef.current = null
                        }
                        setStatus(statusData.status === 'COMPLETED' ? 'success' : 'error')
                        if (statusData.status === 'COMPLETED') {
                            onDocumentProcessed?.()
                        } else {
                            setError('Document processing failed')
                        }
                    }
                } catch (error) {
                    console.error('Error polling status:', error)
                }
            }

            pollStatus()
            pollingIntervalRef.current = setInterval(pollStatus, 2000) as unknown as number

            return () => {
                if (pollingIntervalRef.current) {
                    clearInterval(pollingIntervalRef.current)
                }
            }
        }
    }, [status, documentId])

    return (
        <div className="upload-container">
            <div {...getRootProps()} className={`dropzone ${isDragActive ? 'drag-active' : ''} ${status === 'uploading' || status === 'processing' ? 'disabled' : ''}`}>
                <input {...getInputProps()} />
                <div className="dropzone-content">
                    {isDragActive ? (
                        <p>Drop the PDF here...</p>
                    ) : (
                        <p>Drag & drop a PDF here, or click to select</p>
                    )}
                </div>
            </div>

            {file && (
                <div className="file-info">
                    <button className="dismiss-button" onClick={() => setFile(null)} title="Dismiss">
                        ×
                    </button>
                    <p>Selected: {file.name}</p>
                    <p className="file-size">{(file.size / (1024 * 1024)).toFixed(2)} MB</p>
                </div>
            )}

            {error && (
                <div className="error-message">
                    {error}
                </div>
            )}

            {status === 'uploading' && (
                <div className="upload-status">
                    <div className="spinner-small"></div>
                    <p>Uploading document...</p>
                </div>
            )}

            {status === 'processing' && (
                <div className="processing-status">
                    <div className="spinner-small"></div>
                    <p>Processing document...</p>
                </div>
            )}

            {status === 'success' && (
                <div className="success-message">
                    <button
                        className="dismiss-button"
                        onClick={() => {
                            setStatus('idle')
                            setFile(null)
                        }}
                        title="Dismiss"
                    >
                        ×
                    </button>
                    <p>✅ Document processed successfully!</p>
                </div>
            )}
        </div>
    )
}

export default FileUpload;