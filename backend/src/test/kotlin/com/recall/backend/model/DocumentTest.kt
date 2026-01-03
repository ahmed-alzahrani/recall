package com.recall.backend.model

import com.recall.backend.annotations.DatabaseTest
import com.recall.backend.repository.DocumentRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DatabaseTest
class DocumentTest {
    @Autowired lateinit var documentRepository: DocumentRepository

    @Test
    fun `should create document with all required fields`() {
        val savedDocument = createDocument()

        assertNotNull(savedDocument)
        assertNotNull(savedDocument.id)
        assertEquals("test-document.pdf", savedDocument.filename)
        assertEquals(DocumentStatus.PENDING, savedDocument.status)
        assertNull(savedDocument.totalChunks)
        assertNull(savedDocument.summary)
    }

    @Test
    fun `should set timestamps on persist`() {
        val savedDocument = createDocument()

        assertNotNull(savedDocument.createdAt)
        assertNotNull(savedDocument.updatedAt)
    }

    @Test
    fun `should update timestamp on update`() {
        val savedDocument = createDocument()
        val originalCreatedAt = savedDocument.createdAt
        val originalUpdatedAt = savedDocument.updatedAt
        Thread.sleep(10)

        savedDocument.filename = "updated-document.pdf"
        val updatedDocument = documentRepository.save(savedDocument)

        documentRepository.flush()
        assertEquals(originalCreatedAt, updatedDocument.createdAt)
        assertTrue(updatedDocument.updatedAt.isAfter(originalUpdatedAt))
    }

    @Test
    fun `should handle optional total chunks`() {
        val savedDocument = createDocument(totalChunks = 10)
        assertEquals(10, savedDocument.totalChunks)
    }

    @Test
    fun `should handle optional summary`() {
        val summary = "This is a test document summary"
        val savedDocument = createDocument(summary = summary)
        assertEquals(summary, savedDocument.summary)
    }

    @Test
    fun `should handle different status values`() {
        val pending = createDocument(status = DocumentStatus.PENDING)
        val processing = createDocument(status = DocumentStatus.PROCESSING)
        val completed = createDocument(status = DocumentStatus.COMPLETED)
        val failed = createDocument(status = DocumentStatus.FAILED)

        assertEquals(DocumentStatus.PENDING, pending.status)
        assertEquals(DocumentStatus.PROCESSING, processing.status)
        assertEquals(DocumentStatus.COMPLETED, completed.status)
        assertEquals(DocumentStatus.FAILED, failed.status)
    }

    @Test
    fun `should maintain relationship with chunks`() {
        val savedDocument = createDocument()
        assertNotNull(savedDocument.chunks)
        assertTrue(savedDocument.chunks.isEmpty())
    }

    private fun createDocument(
            filename: String = "test-document.pdf",
            status: DocumentStatus = DocumentStatus.PENDING,
            totalChunks: Int? = null,
            summary: String? = null,
    ) =
            documentRepository.save(
                    Document().apply {
                        this.filename = filename
                        this.status = status
                        this.totalChunks = totalChunks
                        this.summary = summary
                    }
            )
}
