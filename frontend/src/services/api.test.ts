import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  uploadDocument,
  getAllDocuments,
  getDocumentStatus,
  getDocument,
  chatWithDocument,
} from './api'

describe('api', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('uploadDocument', () => {
    it('should upload file and return document data', async () => {
      const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' })
      const mockResponse = { documentId: 123, message: 'File uploaded successfully' }

      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      } as Response)

      const result = await uploadDocument(mockFile)

      expect(result).toEqual(mockResponse)
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/documents/upload',
        expect.objectContaining({
          method: 'POST',
          body: expect.any(FormData),
        })
      )
    })

    it('should throw error with message from API', async () => {
      const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' })
      const mockError = { error: 'File too large' }

      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => mockError,
      } as Response)

      await expect(uploadDocument(mockFile)).rejects.toThrow('File too large')
    })

    it('should throw generic error when API error has no message', async () => {
      const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' })

      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => ({}),
      } as Response)

      await expect(uploadDocument(mockFile)).rejects.toThrow('Upload failed')
    })
  })

  describe('getAllDocuments', () => {
    it('should fetch and return documents', async () => {
      const mockDocuments = [
        { documentId: 1, filename: 'doc1.pdf', status: 'COMPLETED' },
        { documentId: 2, filename: 'doc2.pdf', status: 'PENDING' },
      ]

      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => mockDocuments,
      } as Response)

      const result = await getAllDocuments()

      expect(result).toEqual(mockDocuments)
      expect(fetch).toHaveBeenCalledWith('http://localhost:8080/api/documents')
    })

    it('should throw error on failed request', async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
      } as Response)

      await expect(getAllDocuments()).rejects.toThrow('Failed to fetch documents')
    })
  })

  describe('getDocumentStatus', () => {
    it('should fetch and return document status', async () => {
      const mockStatus = { documentId: 123, status: 'PROCESSING', filename: 'test.pdf' }

      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => mockStatus,
      } as Response)

      const result = await getDocumentStatus(123)

      expect(result).toEqual(mockStatus)
      expect(fetch).toHaveBeenCalledWith('http://localhost:8080/api/documents/123/status')
    })

    it('should throw error on failed request', async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
      } as Response)

      await expect(getDocumentStatus(123)).rejects.toThrow('Failed to fetch document status')
    })
  })

  describe('getDocument', () => {
    it('should fetch and return document', async () => {
      const mockDocument = {
        documentId: 123,
        filename: 'test.pdf',
        status: 'COMPLETED',
        summary: 'Test summary',
      }

      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => mockDocument,
      } as Response)

      const result = await getDocument(123)

      expect(result).toEqual(mockDocument)
      expect(fetch).toHaveBeenCalledWith('http://localhost:8080/api/documents/123')
    })

    it('should throw error on failed request', async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
      } as Response)

      await expect(getDocument(123)).rejects.toThrow('Failed to fetch document')
    })
  })

  describe('chatWithDocument', () => {
    it('should send question and return answer', async () => {
      const mockResponse = { answer: 'This is the answer' }

      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      } as Response)

      const result = await chatWithDocument(123, 'What is this about?')

      expect(result).toEqual(mockResponse)
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/documents/123/chat',
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify('What is this about?'),
        })
      )
    })

    it('should throw error with message from API', async () => {
      const mockError = { error: 'Question too long' }

      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => mockError,
      } as Response)

      await expect(chatWithDocument(123, 'Question')).rejects.toThrow('Question too long')
    })

    it('should throw generic error when API error has no message', async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => ({}),
      } as Response)

      await expect(chatWithDocument(123, 'Question')).rejects.toThrow('Failed to get answer')
    })
  })
})
