import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FileUpload from './FileUpload'
import * as api from '../../services/api'

vi.mock('../../services/api')

let mockOnDrop: (acceptedFiles: File[], rejectedFiles: any[]) => void
vi.mock('react-dropzone', () => ({
    useDropzone: (config: any) => {
        mockOnDrop = config.onDrop
        return {
            getRootProps: () => ({
                onClick: () => { },
            }),
            getInputProps: () => ({}),
            isDragActive: false,
        }
    },
}))

describe('FileUpload', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    const createMockFile = (name: string, size: number, type = 'application/pdf') => {
        const file = new File(['content'], name, { type })
        Object.defineProperty(file, 'size', { value: size })
        return file
    }

    it('should render dropzone', () => {
        render(<FileUpload />)
        expect(screen.getByText(/Drag & drop a PDF here/i)).toBeInTheDocument()
    })

    it('should upload file and start processing', async () => {
        vi.mocked(api.uploadDocument).mockResolvedValue({ documentId: 123 })
        vi.mocked(api.getDocumentStatus).mockResolvedValue({ status: 'PROCESSING' })

        render(<FileUpload />)

        const file = createMockFile('test.pdf', 1024)

        await act(async () => {
            await mockOnDrop([file], [])
        })

        await waitFor(() => {
            expect(api.uploadDocument).toHaveBeenCalledWith(file)
            expect(screen.getByText('Processing document...')).toBeInTheDocument()
        })
    })

    it('should show error for non-PDF file', async () => {
        render(<FileUpload />)

        const file = createMockFile('test.txt', 1024, 'text/plain')

        await act(async () => {
            await mockOnDrop([], [{ errors: [{ code: 'file-invalid-type' }] }])
        })

        await waitFor(() => {
            expect(screen.getByText(/Only PDF files are allowed/i)).toBeInTheDocument()
        })
    })

    it('should show error for file exceeding size limit', async () => {
        render(<FileUpload />)

        await act(async () => {
            await mockOnDrop([], [{ errors: [{ code: 'file-too-large' }] }])
        })

        await waitFor(() => {
            expect(screen.getByText(/File size exceeds 65MB limit/i)).toBeInTheDocument()
        })
    })

    it('should poll document status every 2 seconds', async () => {
        vi.mocked(api.uploadDocument).mockResolvedValue({ documentId: 123 })
        vi.mocked(api.getDocumentStatus)
            .mockResolvedValueOnce({ status: 'PROCESSING' })
            .mockResolvedValueOnce({ status: 'PROCESSING' })

        render(<FileUpload />)

        const file = createMockFile('test.pdf', 1024)

        await act(async () => {
            await mockOnDrop([file], [])
        })

        await waitFor(() => {
            expect(api.uploadDocument).toHaveBeenCalled()
        })

        await waitFor(() => {
            expect(api.getDocumentStatus).toHaveBeenCalledWith(123)
        }, { timeout: 3000 })

        expect(api.getDocumentStatus).toHaveBeenCalledTimes(1)
    })

    it('should stop polling and show success when status is COMPLETED', async () => {
        const mockOnDocumentProcessed = vi.fn()
        vi.mocked(api.uploadDocument).mockResolvedValue({ documentId: 123 })
        vi.mocked(api.getDocumentStatus).mockResolvedValue({ status: 'COMPLETED' })

        render(<FileUpload onDocumentProcessed={mockOnDocumentProcessed} />)

        const file = createMockFile('test.pdf', 1024)

        await act(async () => {
            await mockOnDrop([file], [])
        })

        await waitFor(() => {
            expect(api.uploadDocument).toHaveBeenCalled()
        })

        await waitFor(() => {
            expect(screen.getByText(/Document processed successfully/i)).toBeInTheDocument()
            expect(mockOnDocumentProcessed).toHaveBeenCalled()
        }, { timeout: 3000 })

        const initialCallCount = vi.mocked(api.getDocumentStatus).mock.calls.length

        await new Promise(resolve => setTimeout(resolve, 2500))

        expect(vi.mocked(api.getDocumentStatus).mock.calls.length).toBe(initialCallCount)
    })

    it('should stop polling and show error when status is FAILED', async () => {
        vi.mocked(api.uploadDocument).mockResolvedValue({ documentId: 123 })
        vi.mocked(api.getDocumentStatus).mockResolvedValue({ status: 'FAILED' })

        render(<FileUpload />)

        const file = createMockFile('test.pdf', 1024)

        await act(async () => {
            await mockOnDrop([file], [])
        })

        await waitFor(() => {
            expect(api.uploadDocument).toHaveBeenCalled()
        })

        await waitFor(() => {
            expect(screen.getByText(/Document processing failed/i)).toBeInTheDocument()
        }, { timeout: 3000 })
    })

    it('should handle upload API error', async () => {
        vi.mocked(api.uploadDocument).mockRejectedValue(new Error('Upload failed'))

        render(<FileUpload />)

        const file = createMockFile('test.pdf', 1024)

        await act(async () => {
            await mockOnDrop([file], [])
        })

        await waitFor(() => {
            expect(screen.getByText(/Upload failed/i)).toBeInTheDocument()
        })
    })

    it('should show selected file info', async () => {
        render(<FileUpload />)

        const file = createMockFile('test.pdf', 1024 * 1024)

        await act(async () => {
            await mockOnDrop([file], [])
        })

        await waitFor(() => {
            expect(screen.getByText(/Selected: test\.pdf/i)).toBeInTheDocument()
            expect(screen.getByText(/1\.00 MB/i)).toBeInTheDocument()
        })
    })

    it('should dismiss selected file', async () => {
        const user = userEvent.setup()
        render(<FileUpload />)

        const file = createMockFile('test.pdf', 1024)

        await act(async () => {
            await mockOnDrop([file], [])
        })

        await waitFor(() => {
            expect(screen.getByText(/Selected:/i)).toBeInTheDocument()
        })

        const dismissButtons = screen.getAllByTitle('Dismiss')
        await user.click(dismissButtons[0])

        await waitFor(() => {
            expect(screen.queryByText(/Selected:/i)).not.toBeInTheDocument()
        })
    })

    it('should clean up polling interval on unmount', async () => {
        vi.mocked(api.uploadDocument).mockResolvedValue({ documentId: 123 })
        vi.mocked(api.getDocumentStatus).mockResolvedValue({ status: 'PROCESSING' })

        const { unmount } = render(<FileUpload />)

        const file = createMockFile('test.pdf', 1024)

        await act(async () => {
            await mockOnDrop([file], [])
        })

        await waitFor(() => {
            expect(api.uploadDocument).toHaveBeenCalled()
        })

        const callCountBeforeUnmount = vi.mocked(api.getDocumentStatus).mock.calls.length

        unmount()

        await new Promise(resolve => setTimeout(resolve, 2500))

        expect(vi.mocked(api.getDocumentStatus).mock.calls.length).toBe(callCountBeforeUnmount)
    })

    it('should handle polling errors gracefully', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => { })
        vi.mocked(api.uploadDocument).mockResolvedValue({ documentId: 123 })
        vi.mocked(api.getDocumentStatus).mockRejectedValue(new Error('Polling error'))

        render(<FileUpload />)

        const file = createMockFile('test.pdf', 1024)

        await act(async () => {
            await mockOnDrop([file], [])
        })

        await waitFor(() => {
            expect(api.uploadDocument).toHaveBeenCalled()
        })

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalled()
        }, { timeout: 3000 })

        consoleErrorSpy.mockRestore()
    })
})

