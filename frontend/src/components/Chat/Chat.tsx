import { useState, useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import { getDocument, chatWithDocument } from '../../services/api'
import type { Document } from '../../types/document'
import './Chat.css'

interface Message {
    id: string
    type: 'user' | 'assistant'
    content: string
    timestamp: Date
}

function Chat() {
    const { documentId } = useParams<{ documentId: string }>()

    const [document, setDocument] = useState<Document | null>(null)
    const [messages, setMessages] = useState<Message[]>([])
    const [inputValue, setInputValue] = useState('')
    const [loading, setLoading] = useState(false)
    const [sending, setSending] = useState(false)
    const messagesEndRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        const fetchDocument = async () => {
            if (!documentId) return

            try {
                setLoading(true)
                const doc = await getDocument(parseInt(documentId))
                setDocument(doc)
            } catch (error) {
                console.error('Failed to fetch document:', error)
            } finally {
                setLoading(false)
            }
        }

        fetchDocument()
    }, [documentId])

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, [messages])

    const handleSend = async () => {
        if (!inputValue.trim() || !documentId || sending) return

        const question = inputValue.trim()
        setInputValue('')

        const userMessage: Message = {
            id: Date.now().toString(),
            type: 'user',
            content: question,
            timestamp: new Date(),
        }

        setMessages((prev) => [...prev, userMessage])
        setSending(true)

        try {
            const response = await chatWithDocument(parseInt(documentId), question)
            const assistantMessage: Message = {
                id: (Date.now() + 1).toString(),
                type: 'assistant',
                content: response.answer,
                timestamp: new Date(),
            }
            setMessages((prev) => [...prev, assistantMessage])
        } catch (error) {
            console.error('Failed to get answer:', error)
            const errorMessage: Message = {
                id: (Date.now() + 1).toString(),
                type: 'assistant',
                content: error instanceof Error ? error.message : 'Failed to get answer. Please try again.',
                timestamp: new Date(),
            }
            setMessages((prev) => [...prev, errorMessage])
        } finally {
            setSending(false)
        }
    }

    return (
        <div className="chat-container">
            {loading && (
                <div className="chat-loading">
                    <div className="spinner"></div>
                    <p>Loading document...</p>
                </div>
            )}

            {!loading && !document && (
                <div>Document not found</div>
            )}

            {!loading && document && (
                <>
                    <div className="chat-header">
                        <h1>{document.filename}</h1>
                        {document.summary && (
                            <p className="document-summary">{document.summary}</p>
                        )}
                    </div>

                    <div className="chat-messages">
                        {messages.length === 0 ? (
                            <div className="chat-empty">
                                <p>Ask a question about this document to get started.</p>
                            </div>
                        ) : (
                            messages.map((message) => (
                                <div key={message.id} className={`message message-${message.type}`}>
                                    <div className="message-label">
                                        {message.type === 'user' ? 'You' : 'Assistant'}
                                    </div>
                                    <div className="message-content">
                                        {message.type === 'assistant' ? (
                                            <ReactMarkdown>{message.content}</ReactMarkdown>
                                        ) : (
                                            message.content
                                        )}
                                    </div>
                                </div>
                            ))
                        )}
                        <div ref={messagesEndRef} />
                    </div>
                    <div className="chat-input-container">
                        <textarea
                            className="chat-input"
                            value={inputValue}
                            onChange={(e) => setInputValue(e.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter' && !e.shiftKey) {
                                    e.preventDefault()
                                    handleSend()
                                }
                            }}
                            placeholder="Ask a question about this document..."
                            rows={3}
                            disabled={sending}
                        />
                        <div className="chat-input-footer">
                            <button
                                className="chat-send-button"
                                onClick={handleSend}
                                disabled={!inputValue.trim() || sending}
                                title="Send message"
                            >
                                {sending ? '...' : '↑'}
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    )

}

export default Chat