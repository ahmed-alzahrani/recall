package com.recall.backend.controller

import com.recall.backend.model.Chunk
import com.recall.backend.model.Document
import com.recall.backend.model.DocumentStatus
import com.recall.backend.repository.ChunkRepository
import com.recall.backend.repository.DocumentRepository
import com.recall.backend.service.AnswerService
import com.recall.backend.service.EmbeddingService
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.web.multipart.MultipartFile

class DocumentControllerTest {

    @TempDir lateinit var tempDir: Path

    private lateinit var mockDocumentRepository: DocumentRepository
    private lateinit var mockRabbitTemplate: RabbitTemplate
    private lateinit var mockEmbeddingService: EmbeddingService
    private lateinit var mockChunkRepository: ChunkRepository
    private lateinit var mockAnswerService: AnswerService
    private lateinit var documentController: DocumentController

    @BeforeEach
    fun setUp() {
        mockDocumentRepository = mock(DocumentRepository::class.java)
        mockRabbitTemplate = mock(RabbitTemplate::class.java)
        mockEmbeddingService = mock(EmbeddingService::class.java)
        mockChunkRepository = mock(ChunkRepository::class.java)
        mockAnswerService = mock(AnswerService::class.java)
        documentController =
                DocumentController(
                        mockDocumentRepository,
                        mockRabbitTemplate,
                        mockEmbeddingService,
                        mockChunkRepository,
                        mockAnswerService,
                        tempDir.toString()
                )
    }

    @Test
    fun `should upload PDF document successfully`() {
        val mockFile = mock(MultipartFile::class.java)
        val document =
                Document().apply {
                    id = 1L
                    filename = "test.pdf"
                    status = DocumentStatus.PENDING
                }

        `when`(mockFile.contentType).thenReturn("application/pdf")
        `when`(mockFile.size).thenReturn(1024L)
        `when`(mockFile.originalFilename).thenReturn("test.pdf")
        `when`(mockDocumentRepository.save(any(Document::class.java))).thenReturn(document)
        doNothing().`when`(mockFile).transferTo(any(Path::class.java))

        val result = documentController.uploadDocument(mockFile)

        assertEquals("File uploaded successfully", result["message"])
        assertEquals("test.pdf", result["filename"])
        assertEquals(1024L, result["size"])
        assertEquals(1L, result["documentId"])
        verify(mockDocumentRepository).save(any(Document::class.java))
        verify(mockRabbitTemplate).convertAndSend("document.processing", 1L)
        verify(mockFile).transferTo(any(Path::class.java))
    }

    @Test
    fun `should reject non-PDF file`() {
        val mockFile = mock(MultipartFile::class.java)
        `when`(mockFile.contentType).thenReturn("image/jpeg")

        val result = documentController.uploadDocument(mockFile)

        assertEquals("Only PDF files are allowed", result["error"])
        verifyNoInteractions(mockDocumentRepository)
        verifyNoInteractions(mockRabbitTemplate)
    }

    @Test
    fun `should reject file exceeding size limit`() {
        val mockFile = mock(MultipartFile::class.java)
        val maxSize = 65 * 1024 * 1024L + 1

        `when`(mockFile.contentType).thenReturn("application/pdf")
        `when`(mockFile.size).thenReturn(maxSize)

        val result = documentController.uploadDocument(mockFile)

        assertEquals("File size exceeds 65MB limit", result["error"])
        verify(mockDocumentRepository, never()).save(any())
    }

    @Test
    fun `should use unknown pdf when originalFilename is null`() {
        val mockFile = mock(MultipartFile::class.java)
        val document =
                Document().apply {
                    id = 1L
                    filename = "unknown.pdf"
                    status = DocumentStatus.PENDING
                }

        `when`(mockFile.contentType).thenReturn("application/pdf")
        `when`(mockFile.size).thenReturn(1024L)
        `when`(mockFile.originalFilename).thenReturn(null)
        `when`(mockDocumentRepository.save(any(Document::class.java))).thenReturn(document)
        doNothing().`when`(mockFile).transferTo(any(Path::class.java))

        val result = documentController.uploadDocument(mockFile)

        assertEquals("File uploaded successfully", result["message"])
        assertEquals(null, result["filename"])
        verify(mockDocumentRepository).save(argThat<Document> { it.filename == "unknown.pdf" })
    }

    @Test
    fun `should return all documents`() {
        val documents =
                listOf(
                        Document().apply {
                            id = 1L
                            filename = "doc1.pdf"
                            status = DocumentStatus.COMPLETED
                            summary = "Summary 1"
                            totalChunks = 10
                            createdAt = LocalDateTime.now()
                            updatedAt = LocalDateTime.now()
                        },
                        Document().apply {
                            id = 2L
                            filename = "doc2.pdf"
                            status = DocumentStatus.PENDING
                            summary = null
                            totalChunks = null
                            createdAt = LocalDateTime.now()
                            updatedAt = LocalDateTime.now()
                        }
                )

        `when`(mockDocumentRepository.findAll()).thenReturn(documents)

        val result = documentController.getDocuments()

        assertEquals(2, result.size)
        assertEquals(1L, result[0]["documentId"])
        assertEquals("doc1.pdf", result[0]["filename"])
        assertEquals("COMPLETED", result[0]["status"])
        assertEquals("Summary 1", result[0]["summary"])
        assertEquals(10, result[0]["totalChunks"])
        assertEquals(2L, result[1]["documentId"])
        assertEquals("PENDING", result[1]["status"])
    }

    @Test
    fun `should return empty list when no documents exist`() {
        `when`(mockDocumentRepository.findAll()).thenReturn(emptyList())

        val result = documentController.getDocuments()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return document status`() {
        val document =
                Document().apply {
                    id = 1L
                    filename = "test.pdf"
                    status = DocumentStatus.PROCESSING
                    totalChunks = 5
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }

        `when`(mockDocumentRepository.findById(1L)).thenReturn(Optional.of(document))

        val result = documentController.getDocumentStatus(1L)

        assertEquals(1L, result["documentId"])
        assertEquals("PROCESSING", result["status"])
        assertEquals("test.pdf", result["filename"])
        assertEquals(5, result["totalChunks"])
    }

    @Test
    fun `should throw exception when document not found for status`() {
        `when`(mockDocumentRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) { documentController.getDocumentStatus(999L) }
    }

    @Test
    fun `should return document details`() {
        val document =
                Document().apply {
                    id = 1L
                    filename = "test.pdf"
                    status = DocumentStatus.COMPLETED
                    summary = "Document summary"
                    totalChunks = 15
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }

        `when`(mockDocumentRepository.findById(1L)).thenReturn(Optional.of(document))

        val result = documentController.getDocument(1L)

        assertEquals(1L, result["documentId"])
        assertEquals("test.pdf", result["filename"])
        assertEquals("COMPLETED", result["status"])
        assertEquals("Document summary", result["summary"])
        assertEquals(15, result["totalChunks"])
    }

    @Test
    fun `should throw exception when document not found`() {
        `when`(mockDocumentRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) { documentController.getDocument(999L) }
    }

    @Test
    fun `should answer question for completed document`() {
        val document =
                Document().apply {
                    id = 1L
                    status = DocumentStatus.COMPLETED
                }
        val question = "What is this document about?"
        val embeddedQuestion = FloatArray(768) { 0.1f }
        val chunks =
                listOf(
                        Chunk().apply { chunkText = "Chunk 1 text" },
                        Chunk().apply { chunkText = "Chunk 2 text" }
                )
        val answer = "This document is about testing."

        `when`(mockDocumentRepository.findByIdAndStatus(1L, DocumentStatus.COMPLETED))
                .thenReturn(Optional.of(document))
        `when`(mockEmbeddingService.embedQuestion(question)).thenReturn(embeddedQuestion)
        `when`(mockChunkRepository.findSimilarChunks(1L, embeddedQuestion)).thenReturn(chunks)
        `when`(mockAnswerService.answerQuestion(question, listOf("Chunk 1 text", "Chunk 2 text")))
                .thenReturn(answer)

        val result = documentController.chat(1L, question)

        assertEquals(answer, result["answer"])
        verify(mockEmbeddingService).embedQuestion(question)
        verify(mockChunkRepository).findSimilarChunks(1L, embeddedQuestion)
        verify(mockAnswerService).answerQuestion(question, listOf("Chunk 1 text", "Chunk 2 text"))
    }

    @Test
    fun `should reject question exceeding maximum length`() {
        val longQuestion = "a".repeat(1001)

        val result = documentController.chat(1L, longQuestion)

        assertEquals("Question exceeds maximum length of 1000 characters", result["error"])
        verifyNoInteractions(mockDocumentRepository)
        verifyNoInteractions(mockEmbeddingService)
    }

    @Test
    fun `should throw exception when document not found for chat`() {
        val question = "What is this about?"
        `when`(mockDocumentRepository.findByIdAndStatus(999L, DocumentStatus.COMPLETED))
                .thenReturn(Optional.empty())

        val exception =
                assertThrows(RuntimeException::class.java) {
                    documentController.chat(999L, question)
                }
        assertTrue(exception.message?.contains("No completed document found") == true)
    }

    @Test
    fun `should throw exception when document not completed for chat`() {
        val question = "What is this about?"
        `when`(mockDocumentRepository.findByIdAndStatus(1L, DocumentStatus.COMPLETED))
                .thenReturn(Optional.empty())

        val exception =
                assertThrows(RuntimeException::class.java) { documentController.chat(1L, question) }
        assertTrue(exception.message?.contains("No completed document found") == true)
    }

    @Test
    fun `should handle question at maximum length`() {
        val question = "a".repeat(1000)
        val document =
                Document().apply {
                    id = 1L
                    status = DocumentStatus.COMPLETED
                }
        val embeddedQuestion = FloatArray(768) { 0.1f }
        val chunks = listOf(Chunk().apply { chunkText = "Chunk text" })
        val answer = "Answer"

        `when`(mockDocumentRepository.findByIdAndStatus(1L, DocumentStatus.COMPLETED))
                .thenReturn(Optional.of(document))
        `when`(mockEmbeddingService.embedQuestion(question)).thenReturn(embeddedQuestion)
        `when`(mockChunkRepository.findSimilarChunks(1L, embeddedQuestion)).thenReturn(chunks)
        `when`(mockAnswerService.answerQuestion(question, listOf("Chunk text"))).thenReturn(answer)

        val result = documentController.chat(1L, question)

        assertEquals(answer, result["answer"])
    }
}
