import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import Chat from './Chat'
import * as api from '../../services/api'

vi.mock('../../services/api')
vi.mock('react-markdown', () => ({
    default: ({ children }: { children: string }) => <div>{children}</div>,
}))

beforeEach(() => {
    Element.prototype.scrollIntoView = vi.fn()
})

const mockDocument = {
    documentId: 123,
    filename: 'test-document.pdf',
    status: 'COMPLETED',
    summary: 'This is a test document summary',
    totalChunks: 10,
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
}

describe('Chat', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    const renderChat = (documentId = '123') => {
        return render(
            <MemoryRouter initialEntries={[`/chat/${documentId}`]}>
                <Routes>
                    <Route path="/chat/:documentId" element={<Chat />} />
                </Routes>
            </MemoryRouter>
        )
    }

    it('should show loading state while fetching document', async () => {
        vi.mocked(api.getDocument).mockImplementation(() => new Promise(() => { }))

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('Loading document...')).toBeInTheDocument()
        })
    })

    it('should fetch and display document on mount', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)

        renderChat('123')

        await waitFor(() => {
            expect(api.getDocument).toHaveBeenCalledWith(123)
        })

        expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        expect(screen.getByText('This is a test document summary')).toBeInTheDocument()
    })

    it('should show document not found when fetch fails', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => { })
        vi.mocked(api.getDocument).mockRejectedValue(new Error('Document not found'))

        renderChat('999')

        await waitFor(() => {
            expect(screen.getByText('Document not found')).toBeInTheDocument()
        })

        consoleErrorSpy.mockRestore()
    })

    it('should display empty state when no messages', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('Ask a question about this document to get started.')).toBeInTheDocument()
        })
    })

    it('should add user message and send question to API', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)
        vi.mocked(api.chatWithDocument).mockResolvedValue({ answer: 'This is the answer' })

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const textarea = screen.getByPlaceholderText('Ask a question about this document...')
        const sendButton = screen.getByTitle('Send message')

        await userEvent.type(textarea, 'What is this about?')
        await userEvent.click(sendButton)

        expect(screen.getByText('What is this about?')).toBeInTheDocument()
        expect(api.chatWithDocument).toHaveBeenCalledWith(123, 'What is this about?')
    })

    it('should display assistant response after sending question', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)
        vi.mocked(api.chatWithDocument).mockResolvedValue({ answer: 'This is the answer' })

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const textarea = screen.getByPlaceholderText('Ask a question about this document...')
        const sendButton = screen.getByTitle('Send message')

        await userEvent.type(textarea, 'What is this about?')
        await userEvent.click(sendButton)

        await waitFor(() => {
            expect(screen.getByText('This is the answer')).toBeInTheDocument()
        })

        expect(screen.getByText('You')).toBeInTheDocument()
        expect(screen.getByText('Assistant')).toBeInTheDocument()
    })

    it('should clear input after sending message', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)
        vi.mocked(api.chatWithDocument).mockResolvedValue({ answer: 'Answer' })

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const textarea = screen.getByPlaceholderText('Ask a question about this document...') as HTMLTextAreaElement
        const sendButton = screen.getByTitle('Send message')

        await userEvent.type(textarea, 'Question')
        await userEvent.click(sendButton)

        await waitFor(() => {
            expect(textarea.value).toBe('')
        })
    })

    it('should send message on Enter key press', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)
        vi.mocked(api.chatWithDocument).mockResolvedValue({ answer: 'Answer' })

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const textarea = screen.getByPlaceholderText('Ask a question about this document...')

        await userEvent.type(textarea, 'Question')
        await userEvent.keyboard('{Enter}')

        await waitFor(() => {
            expect(api.chatWithDocument).toHaveBeenCalledWith(123, 'Question')
        })
    })

    it('should not send message on Shift+Enter', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const textarea = screen.getByPlaceholderText('Ask a question about this document...') as HTMLTextAreaElement

        await userEvent.type(textarea, 'Question')
        await userEvent.keyboard('{Shift>}{Enter}{/Shift}')

        expect(api.chatWithDocument).not.toHaveBeenCalled()
    })

    it('should not send empty message', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const sendButton = screen.getByTitle('Send message') as HTMLButtonElement
        expect(sendButton.disabled).toBe(true)

        const textarea = screen.getByPlaceholderText('Ask a question about this document...')
        await userEvent.type(textarea, '   ')
        expect(sendButton.disabled).toBe(true)
    })

    it('should disable input while sending', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)
        vi.mocked(api.chatWithDocument).mockImplementation(() => new Promise(() => { }))

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const textarea = screen.getByPlaceholderText('Ask a question about this document...') as HTMLTextAreaElement
        const sendButton = screen.getByTitle('Send message') as HTMLButtonElement

        await userEvent.type(textarea, 'Question')
        await userEvent.click(sendButton)

        expect(textarea.disabled).toBe(true)
        expect(sendButton.disabled).toBe(true)
    })

    it('should show thinking indicator while sending', async () => {
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)
        vi.mocked(api.chatWithDocument).mockImplementation(() => new Promise(() => { }))

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const textarea = screen.getByPlaceholderText('Ask a question about this document...')
        const sendButton = screen.getByTitle('Send message')

        await userEvent.type(textarea, 'Question')
        await userEvent.click(sendButton)

        await waitFor(() => {
            expect(screen.getByText('Thinking...')).toBeInTheDocument()
        })
    })

    it('should display error message when API call fails', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => { })
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)
        vi.mocked(api.chatWithDocument).mockRejectedValue(new Error('API Error'))

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const textarea = screen.getByPlaceholderText('Ask a question about this document...')
        const sendButton = screen.getByTitle('Send message')

        await userEvent.type(textarea, 'Question')
        await userEvent.click(sendButton)

        await waitFor(() => {
            expect(screen.getByText('API Error')).toBeInTheDocument()
        })

        consoleErrorSpy.mockRestore()
    })

    it('should display generic error message when error has no message', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => { })
        vi.mocked(api.getDocument).mockResolvedValue(mockDocument)
        vi.mocked(api.chatWithDocument).mockRejectedValue({})

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        const textarea = screen.getByPlaceholderText('Ask a question about this document...')
        const sendButton = screen.getByTitle('Send message')

        await userEvent.type(textarea, 'Question')
        await userEvent.click(sendButton)

        await waitFor(() => {
            expect(screen.getByText('Failed to get answer. Please try again.')).toBeInTheDocument()
        })

        consoleErrorSpy.mockRestore()
    })

    it('should not show summary if document has none', async () => {
        const documentWithoutSummary = { ...mockDocument, summary: null }
        vi.mocked(api.getDocument).mockResolvedValue(documentWithoutSummary)

        renderChat()

        await waitFor(() => {
            expect(screen.getByText('test-document.pdf')).toBeInTheDocument()
        })

        expect(screen.queryByText('This is a test document summary')).not.toBeInTheDocument()
    })
})

