import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import DocumentList from './DocumentList'
import * as api from '../../services/api'

const mockNavigate = vi.fn()

vi.mock('../../services/api')
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    }
})

describe('DocumentList', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    const renderDocumentList = (refreshTrigger?: number) => {
        return render(
            <MemoryRouter>
                <DocumentList refreshTrigger={refreshTrigger} />
            </MemoryRouter>
        )
    }

    it('should show loading state initially', () => {
        vi.mocked(api.getAllDocuments).mockImplementation(() => new Promise(() => { }))

        renderDocumentList()
        expect(screen.getByText('Loading documents...')).toBeInTheDocument()
    })

    it('should fetch and display documents', async () => {
        const mockDocuments = [
            {
                documentId: 1,
                filename: 'test1.pdf',
                status: 'COMPLETED',
                summary: 'Summary 1',
                totalChunks: 10,
                createdAt: '2024-01-01',
                updatedAt: '2024-01-01',
            },
            {
                documentId: 2,
                filename: 'test2.pdf',
                status: 'PENDING',
                summary: null,
                totalChunks: 0,
                createdAt: '2024-01-02',
                updatedAt: '2024-01-02',
            },
        ]

        vi.mocked(api.getAllDocuments).mockResolvedValue(mockDocuments)

        renderDocumentList()

        await waitFor(() => {
            expect(screen.getByText('Documents: 2')).toBeInTheDocument()
        })

        expect(screen.getByText('test1.pdf')).toBeInTheDocument()
        expect(screen.getByText('test2.pdf')).toBeInTheDocument()
        expect(screen.getByText('Status: COMPLETED')).toBeInTheDocument()
        expect(screen.getByText('Status: PENDING')).toBeInTheDocument()
        expect(screen.getByText('Summary 1')).toBeInTheDocument()
    })

    it('should display document count', async () => {
        const mockDocuments = [
            {
                documentId: 1,
                filename: 'test1.pdf',
                status: 'COMPLETED',
                summary: null,
                totalChunks: 5,
                createdAt: '2024-01-01',
                updatedAt: '2024-01-01',
            },
        ]

        vi.mocked(api.getAllDocuments).mockResolvedValue(mockDocuments)

        renderDocumentList()

        await waitFor(() => {
            expect(screen.getByText('Documents: 1')).toBeInTheDocument()
        })
    })

    it('should show empty state when no documents', async () => {
        vi.mocked(api.getAllDocuments).mockResolvedValue([])

        renderDocumentList()

        await waitFor(() => {
            expect(screen.getByText('Documents: 0')).toBeInTheDocument()
            expect(screen.getByText('No documents yet')).toBeInTheDocument()
        })
    })

    it('should refetch documents when refreshTrigger changes', async () => {
        vi.mocked(api.getAllDocuments).mockResolvedValue([])

        const { rerender } = renderDocumentList(0)

        await waitFor(() => {
            expect(api.getAllDocuments).toHaveBeenCalledTimes(1)
        })

        rerender(
            <MemoryRouter>
                <DocumentList refreshTrigger={1} />
            </MemoryRouter>
        )

        await waitFor(() => {
            expect(api.getAllDocuments).toHaveBeenCalledTimes(2)
        })
    })

    it('should navigate to chat when COMPLETED document is clicked', async () => {
        const mockDocuments = [
            {
                documentId: 123,
                filename: 'completed.pdf',
                status: 'COMPLETED',
                summary: 'Test summary',
                totalChunks: 10,
                createdAt: '2024-01-01',
                updatedAt: '2024-01-01',
            },
        ]

        vi.mocked(api.getAllDocuments).mockResolvedValue(mockDocuments)

        const { container } = renderDocumentList()

        await waitFor(() => {
            expect(screen.getByText('completed.pdf')).toBeInTheDocument()
        })

        const documentItem = container.querySelector('.document-item')
        expect(documentItem).not.toHaveClass('disabled')

        const user = userEvent.setup()
        await user.click(documentItem!)

        expect(mockNavigate).toHaveBeenCalledWith('/chat/123')
    })

    it('should not navigate when non-COMPLETED document is clicked', async () => {
        const mockDocuments = [
            {
                documentId: 456,
                filename: 'pending.pdf',
                status: 'PENDING',
                summary: null,
                totalChunks: 0,
                createdAt: '2024-01-01',
                updatedAt: '2024-01-01',
            },
        ]

        vi.mocked(api.getAllDocuments).mockResolvedValue(mockDocuments)

        const { container } = renderDocumentList()

        await waitFor(() => {
            expect(screen.getByText('pending.pdf')).toBeInTheDocument()
        })

        const documentItem = container.querySelector('.document-item')
        expect(documentItem).toHaveClass('disabled')

        const user = userEvent.setup()
        await user.click(documentItem!)

        expect(mockNavigate).not.toHaveBeenCalled()
    })

    it('should handle API errors gracefully', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => { })
        vi.mocked(api.getAllDocuments).mockRejectedValue(new Error('API Error'))

        renderDocumentList()

        await waitFor(() => {
            expect(screen.queryByText('Loading documents...')).not.toBeInTheDocument()
        })

        expect(screen.getByText('Documents: 0')).toBeInTheDocument()
        expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to fetch documents:', expect.any(Error))

        consoleErrorSpy.mockRestore()
    })
})

