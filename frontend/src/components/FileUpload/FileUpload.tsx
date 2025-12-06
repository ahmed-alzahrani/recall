import { useDropzone } from 'react-dropzone'
import { useState } from 'react';
import './FileUpload.css';
import { uploadDocument } from '../../services/api';

type UploadStatus = 'idle' | 'uploading' | 'success' | 'error';


function FileUpload() {

    const [file, setFile] = useState<File | null>(null);
    const [status, setStatus] = useState<UploadStatus>('idle');
    const [error, setError] = useState<string | null>(null);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        accept: {
            'application/pdf': ['.pdf']
        },
        maxFiles: 1,
        maxSize: 25 * 1024 * 1024, // 25MB
        onDrop: async (acceptedFiles, rejectedFiles) => {
            setError(null)

            if (rejectedFiles.length > 0) {
                const rejection = rejectedFiles[0]
                const errorCode = rejection.errors[0]?.code

                if (errorCode === 'file-too-large') {
                    setError('File size exceeds 25MB limit')
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
                    setStatus('success')
                } catch (error) {
                    setError('Upload failed. Please try again.')
                    setStatus('error')
                }
            }
        }
    })

    return (
        <div className="upload-container">
            <div {...getRootProps()} className={`dropzone ${isDragActive ? 'drag-active' : ''}`}>
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
                    <p>Selected: {file.name}</p>
                    <p className="file-size">{(file.size / (1024 * 1024)).toFixed(2)} MB</p>
                </div>
            )}
            
            {error && (
                <div className="error-message">
                    {error}
                </div>
            )}
        </div>
    )
}

export default FileUpload;