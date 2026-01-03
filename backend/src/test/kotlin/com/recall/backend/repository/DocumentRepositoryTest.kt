package com.recall.backend.repository

import com.recall.backend.annotations.DatabaseTest
import com.recall.backend.model.Document
import com.recall.backend.model.DocumentStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DatabaseTest
class DocumentRepositoryTest {
    @Autowired lateinit var documentRepository: DocumentRepository

    @Test
    fun `should find document by id and status when both match`() {
        val document =
                documentRepository.save(
                        Document().apply {
                            filename = "test.pdf"
                            status = DocumentStatus.COMPLETED
                        }
                )

        val result = documentRepository.findByIdAndStatus(document.id!!, DocumentStatus.COMPLETED)

        assertTrue(result.isPresent)
        assertEquals(document.id, result.get().id)
        assertEquals(DocumentStatus.COMPLETED, result.get().status)
    }

    @Test
    fun `should return empty when status does not match`() {
        val document =
                documentRepository.save(
                        Document().apply {
                            filename = "test.pdf"
                            status = DocumentStatus.PENDING
                        }
                )

        val result = documentRepository.findByIdAndStatus(document.id!!, DocumentStatus.COMPLETED)

        assertFalse(result.isPresent)
    }

    @Test
    fun `should return empty when id does not exist`() {
        val result = documentRepository.findByIdAndStatus(999L, DocumentStatus.COMPLETED)

        assertFalse(result.isPresent)
    }
}
