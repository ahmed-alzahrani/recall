package com.recall.backend.listener

import com.recall.backend.dto.ChunkData
import com.recall.backend.dto.ChunkWithEmbedding
import com.recall.backend.dto.PageText
import com.recall.backend.model.Chunk
import com.recall.backend.model.Document
import com.recall.backend.model.DocumentStatus
import com.recall.backend.repository.ChunkRepository
import com.recall.backend.repository.DocumentRepository
import com.recall.backend.service.ChunkingService
import com.recall.backend.service.DocumentSummaryService
import com.recall.backend.service.EmbeddingService
import com.recall.backend.service.PDFExtractionService
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*

class DocumentProcessingListenerTest {

    @TempDir lateinit var tempDir: Path

    private lateinit var mockPdfExtractionService: PDFExtractionService
    private lateinit var mockChunkingService: ChunkingService
    private lateinit var mockEmbeddingService: EmbeddingService
    private lateinit var mockDocumentRepository: DocumentRepository
    private lateinit var mockChunkRepository: ChunkRepository
    private lateinit var mockDocumentSummaryService: DocumentSummaryService
    private lateinit var documentProcessingListener: DocumentProcessingListener

    @BeforeEach
    fun setUp() {
        mockPdfExtractionService = mock(PDFExtractionService::class.java)
        mockChunkingService = mock(ChunkingService::class.java)
        mockEmbeddingService = mock(EmbeddingService::class.java)
        mockDocumentRepository = mock(DocumentRepository::class.java)
        mockChunkRepository = mock(ChunkRepository::class.java)
        mockDocumentSummaryService = mock(DocumentSummaryService::class.java)
        documentProcessingListener =
                DocumentProcessingListener(
                        mockPdfExtractionService,
                        mockChunkingService,
                        mockEmbeddingService,
                        mockDocumentRepository,
                        mockChunkRepository,
                        mockDocumentSummaryService,
                        tempDir.toString()
                )
    }

    @Test
    fun `should process document successfully`() {
        val documentId = 1L
        val document =
                Document().apply {
                    id = documentId
                    filename = "test.pdf"
                    status = DocumentStatus.PENDING
                }

        val pages = listOf(PageText(1, "Page 1 text"))
        val chunks = listOf(ChunkData("Chunk 1 text", 0, 1, 1), ChunkData("Chunk 2 text", 1, 1, 1))
        val summary = "Document summary"
        val chunksWithEmbeddings =
                listOf(
                        ChunkWithEmbedding(chunks[0], listOf(0.1f, 0.2f)),
                        ChunkWithEmbedding(chunks[1], listOf(0.3f, 0.4f))
                )

        // Create a temporary file to test cleanup
        val tempFile = tempDir.resolve(documentId.toString())
        Files.createFile(tempFile)

        val statusOrder = mutableListOf<DocumentStatus>()
        doAnswer { invocation ->
                    val doc = invocation.getArgument<Document>(0)
                    statusOrder.add(doc.status)
                    doc
                }
                .`when`(mockDocumentRepository)
                .save(any(Document::class.java))

        `when`(mockDocumentRepository.findById(documentId)).thenReturn(Optional.of(document))
        `when`(mockPdfExtractionService.extractText(documentId)).thenReturn(pages)
        `when`(mockChunkingService.chunkDocument(pages)).thenReturn(chunks)
        `when`(mockDocumentSummaryService.generateSummary(chunks)).thenReturn(summary)
        `when`(mockEmbeddingService.embedChunks(chunks)).thenReturn(chunksWithEmbeddings)
        `when`(mockChunkRepository.saveAll(anyList())).thenReturn(emptyList())

        documentProcessingListener.processDocument(documentId)

        verify(mockDocumentRepository, times(2)).save(any(Document::class.java))
        assertEquals(listOf(DocumentStatus.PROCESSING, DocumentStatus.COMPLETED), statusOrder)

        assertEquals(DocumentStatus.COMPLETED, document.status)
        assertEquals(summary, document.summary)
        assertEquals(2, document.totalChunks)

        verify(mockPdfExtractionService).extractText(documentId)
        verify(mockChunkingService).chunkDocument(pages)
        verify(mockDocumentSummaryService).generateSummary(chunks)
        verify(mockEmbeddingService).embedChunks(chunks)
        verify(mockChunkRepository).saveAll(anyList())

        // Verify file was deleted
        assertFalse(Files.exists(tempFile))
    }

    @Test
    fun `should throw exception when document not found`() {
        val documentId = 999L
        `when`(mockDocumentRepository.findById(documentId)).thenReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) {
            documentProcessingListener.processDocument(documentId)
        }

        verify(mockDocumentRepository, never()).save(any())
        verifyNoInteractions(mockPdfExtractionService)
    }

    @Test
    fun `should set status to FAILED when processing fails`() {
        val documentId = 1L
        val document =
                Document().apply {
                    id = documentId
                    filename = "test.pdf"
                    status = DocumentStatus.PENDING
                }

        val statusOrder = mutableListOf<DocumentStatus>()
        doAnswer { invocation ->
                    val doc = invocation.getArgument<Document>(0)
                    statusOrder.add(doc.status)
                    doc
                }
                .`when`(mockDocumentRepository)
                .save(any(Document::class.java))

        `when`(mockDocumentRepository.findById(documentId)).thenReturn(Optional.of(document))
        `when`(mockPdfExtractionService.extractText(documentId))
                .thenThrow(RuntimeException("PDF extraction failed"))

        documentProcessingListener.processDocument(documentId)

        verify(mockDocumentRepository, times(2)).save(any(Document::class.java))
        assertEquals(listOf(DocumentStatus.PROCESSING, DocumentStatus.FAILED), statusOrder)

        assertEquals(DocumentStatus.FAILED, document.status)
        assertNull(document.summary)
        assertNull(document.totalChunks)

        verify(mockChunkRepository, never()).saveAll(anyList())
    }

    @Test
    fun `should handle file cleanup failure gracefully`() {
        val documentId = 1L
        val document =
                Document().apply {
                    id = documentId
                    filename = "test.pdf"
                    status = DocumentStatus.PENDING
                }

        val pages = listOf(PageText(1, "Page 1 text"))
        val chunks = listOf(ChunkData("Chunk text", 0, 1, 1))
        val summary = "Summary"
        val chunksWithEmbeddings = listOf(ChunkWithEmbedding(chunks[0], listOf(0.1f)))

        `when`(mockDocumentRepository.findById(documentId)).thenReturn(Optional.of(document))
        `when`(mockPdfExtractionService.extractText(documentId)).thenReturn(pages)
        `when`(mockChunkingService.chunkDocument(pages)).thenReturn(chunks)
        `when`(mockDocumentSummaryService.generateSummary(chunks)).thenReturn(summary)
        `when`(mockEmbeddingService.embedChunks(chunks)).thenReturn(chunksWithEmbeddings)
        `when`(mockChunkRepository.saveAll(anyList())).thenReturn(emptyList())

        assertDoesNotThrow { documentProcessingListener.processDocument(documentId) }

        val captor = ArgumentCaptor.forClass(Document::class.java)
        verify(mockDocumentRepository, times(2)).save(captor.capture())
        assertEquals(DocumentStatus.COMPLETED, captor.allValues[1].status)
    }

    @Test
    fun `should create chunk entities with correct mapping`() {
        val documentId = 1L
        val document =
                Document().apply {
                    id = documentId
                    filename = "test.pdf"
                    status = DocumentStatus.PENDING
                }

        val pages = listOf(PageText(1, "Page text"))
        val chunks = listOf(ChunkData("Chunk text", 0, 1, 2))
        val chunksWithEmbeddings = listOf(ChunkWithEmbedding(chunks[0], listOf(0.1f, 0.2f)))

        var savedChunks: List<Chunk>? = null
        doAnswer { invocation ->
                    @Suppress("UNCHECKED_CAST")
                    val chunks: List<Chunk> = invocation.getArgument<List<Chunk>>(0)
                    savedChunks = chunks
                    emptyList<Chunk>()
                }
                .`when`(mockChunkRepository)
                .saveAll(anyList())

        `when`(mockDocumentRepository.findById(documentId)).thenReturn(Optional.of(document))
        `when`(mockPdfExtractionService.extractText(documentId)).thenReturn(pages)
        `when`(mockChunkingService.chunkDocument(pages)).thenReturn(chunks)
        `when`(mockDocumentSummaryService.generateSummary(chunks)).thenReturn("Summary")
        `when`(mockEmbeddingService.embedChunks(chunks)).thenReturn(chunksWithEmbeddings)

        documentProcessingListener.processDocument(documentId)

        verify(mockChunkRepository).saveAll(anyList())
        assertNotNull(savedChunks)
        assertEquals(1, savedChunks!!.size)
        val savedChunk = savedChunks!![0]
        assertEquals(document, savedChunk.document)
        assertEquals("Chunk text", savedChunk.chunkText)
        assertEquals(0, savedChunk.chunkIndex)
        assertEquals(1, savedChunk.pageStart)
        assertEquals(2, savedChunk.pageEnd)
        assertArrayEquals(floatArrayOf(0.1f, 0.2f), savedChunk.embedding)
    }

    @Test
    fun `should handle exception during chunk saving`() {
        val documentId = 1L
        val document =
                Document().apply {
                    id = documentId
                    status = DocumentStatus.PENDING
                }

        val pages = listOf(PageText(1, "Text"))
        val chunks = listOf(ChunkData("Text", 0, 1, 1))
        val chunksWithEmbeddings = listOf(ChunkWithEmbedding(chunks[0], listOf(0.1f)))

        `when`(mockDocumentRepository.findById(documentId)).thenReturn(Optional.of(document))
        `when`(mockPdfExtractionService.extractText(documentId)).thenReturn(pages)
        `when`(mockChunkingService.chunkDocument(pages)).thenReturn(chunks)
        `when`(mockDocumentSummaryService.generateSummary(chunks)).thenReturn("Summary")
        `when`(mockEmbeddingService.embedChunks(chunks)).thenReturn(chunksWithEmbeddings)
        `when`(mockChunkRepository.saveAll(anyList())).thenThrow(RuntimeException("Database error"))

        documentProcessingListener.processDocument(documentId)

        val captor = ArgumentCaptor.forClass(Document::class.java)
        verify(mockDocumentRepository, times(2)).save(captor.capture())
        assertEquals(DocumentStatus.FAILED, captor.allValues[1].status)
    }
}
