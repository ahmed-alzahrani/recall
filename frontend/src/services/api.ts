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