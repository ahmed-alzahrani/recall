import { describe, it, expect, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from './App'

vi.mock('./components/FileUpload', () => ({
    default: ({ onDocumentProcessed }: { onDocumentProcessed?: () => void }) => (
        <div data-testid="file-upload">
            FileUpload Component
            {onDocumentProcessed && (
                <button onClick={onDocumentProcessed}>Trigger Processed</button>
            )}
        </div>
    ),
}))

vi.mock('./components/DocumentList/DocumentList', () => ({
    default: ({ refreshTrigger }: { refreshTrigger?: number }) => (
        <div data-testid="document-list">
            DocumentList Component (trigger: {refreshTrigger})
        </div>
    ),
}))

vi.mock('./components/Chat/Chat', () => ({
    default: () => <div data-testid="chat">Chat Component</div>,
}))

describe('App', () => {
    const renderApp = (initialPath = '/') => {
        return render(
            <MemoryRouter initialEntries={[initialPath]}>
                <App />
            </MemoryRouter>
        )
    }

    it('should render top bar with sidebar toggle, upload link, and GitHub link', () => {
        renderApp()

        expect(screen.getByText('📄 Documents')).toBeInTheDocument()
        expect(screen.getByText('📤 Upload')).toBeInTheDocument()
        expect(screen.getByTitle('View on GitHub')).toBeInTheDocument()
    })

    it('should render GitHub link with correct attributes', () => {
        renderApp()
        const githubLink = screen.getByTitle('View on GitHub')

        expect(githubLink).toHaveAttribute('href', 'https://github.com/ahmed-alzahrani/recall')
        expect(githubLink).toHaveAttribute('target', '_blank')
        expect(githubLink).toHaveAttribute('rel', 'noopener noreferrer')
    })

    it('should toggle sidebar when sidebar toggle button is clicked', async () => {
        const user = userEvent.setup()
        renderApp()

        const toggleButton = screen.getByText('📄 Documents')
        const sidebar = screen.queryByTestId('document-list')

        expect(sidebar).not.toBeInTheDocument()

        await user.click(toggleButton)
        expect(screen.getByTestId('document-list')).toBeInTheDocument()

        await user.click(toggleButton)
        expect(screen.queryByTestId('document-list')).not.toBeInTheDocument()
    })

    it('should render FileUpload component on root path', () => {
        renderApp('/')

        expect(screen.getByTestId('file-upload')).toBeInTheDocument()
        expect(screen.queryByTestId('chat')).not.toBeInTheDocument()
    })

    it('should render Chat component on chat path', () => {
        renderApp('/chat/123')

        expect(screen.getByTestId('chat')).toBeInTheDocument()
        expect(screen.queryByTestId('file-upload')).not.toBeInTheDocument()
    })

    it('should render main heading on non-chat pages', () => {
        renderApp('/')

        expect(screen.getByRole('heading', { name: 'Recall' })).toBeInTheDocument()
    })

    it('should not render main heading on chat pages', () => {
        renderApp('/chat/123')

        expect(screen.queryByRole('heading', { name: 'Recall' })).not.toBeInTheDocument()
    })

    it('should increment refreshTrigger when onDocumentProcessed is called', async () => {
        const user = userEvent.setup()
        renderApp('/')

        const documentList = screen.queryByTestId('document-list')
        expect(documentList).not.toBeInTheDocument()

        await user.click(screen.getByText('📄 Documents'))

        let documentListVisible = screen.getByTestId('document-list')
        expect(documentListVisible).toHaveTextContent('trigger: 0')

        const triggerButton = within(screen.getByTestId('file-upload')).getByText('Trigger Processed')
        await user.click(triggerButton)

        documentListVisible = screen.getByTestId('document-list')
        expect(documentListVisible).toHaveTextContent('trigger: 1')

        await user.click(triggerButton)
        documentListVisible = screen.getByTestId('document-list')
        expect(documentListVisible).toHaveTextContent('trigger: 2')
    })

    it('should pass refreshTrigger to DocumentList component', async () => {
        const user = userEvent.setup()
        renderApp('/')

        await user.click(screen.getByText('📄 Documents'))

        const documentList = screen.getByTestId('document-list')
        expect(documentList).toHaveTextContent('trigger: 0')
    })
})

