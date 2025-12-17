export interface Document {
    documentId: number
    filename: string
    status: string
    summary: string | null
    totalChunks: number
    createdAt: string
    updatedAt: string
  }