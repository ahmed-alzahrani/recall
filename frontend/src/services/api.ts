const API_BASE_URL = 'http://localhost:8080/api';

export async function uploadDocument(file: File): Promise<any> {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await fetch(`${API_BASE_URL}/documents/upload`, {
        method: 'POST',
        body: formData,
    });
    
    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Upload failed');
    }
    
    return response.json();
}

export async function getAllDocuments(): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/documents`);

    if (!response.ok) {
        throw new Error('Failed to fetch documents');
    }

    return response.json();
}

export async function getDocumentStatus(documentId: number): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/documents/${documentId}/status`);
    
    if (!response.ok) {
        throw new Error('Failed to fetch document status');
    }
    
    return response.json();
}

export async function getDocument(documentId: number): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/documents/${documentId}`);
    
    if (!response.ok) {
        throw new Error('Failed to fetch document');
    }
    
    return response.json();
}