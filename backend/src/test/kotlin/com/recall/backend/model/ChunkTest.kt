package com.recall.backend.model

import com.recall.backend.RecallBackendApplication
import com.recall.backend.config.FlywayConfig
import com.recall.backend.repository.ChunkRepository
import com.recall.backend.repository.DocumentRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(classes = [RecallBackendApplication::class])
@Import(FlywayConfig::class)
class ChunkTest {

    private lateinit var document: Document

    @Autowired lateinit var chunkRepository: ChunkRepository
    @Autowired lateinit var documentRepository: DocumentRepository

    @Test
    fun `should create chunk with all required fields`() {
        val savedChunk = createChunk()

        assertNotNull(savedChunk)
        assertNotNull(savedChunk.document?.id)
        assertEquals("Test text", savedChunk.chunkText)
        assertEquals(0, savedChunk.chunkIndex)
        assertNotNull(savedChunk.embedding)
        assertEquals(768, savedChunk.embedding!!.size)
        assertNull(savedChunk.pageStart)
        assertNull(savedChunk.pageEnd)
    }

    @Test
    fun `should set timestamps on persist`() {
        val savedChunk = createChunk()

        assertNotNull(savedChunk.createdAt)
        assertNotNull(savedChunk.updatedAt)
    }

    @Test
    fun `should update timestamp on update`() {
        val savedChunk = createChunk()
        val originalCreatedAt = savedChunk.createdAt
        val originalUpdatedAt = savedChunk.updatedAt
        Thread.sleep(10)

        savedChunk.chunkText = "Updated text"
        val updatedChunk = chunkRepository.save(savedChunk)

        chunkRepository.flush()
        assertEquals(originalCreatedAt, updatedChunk.createdAt)
        assertTrue(updatedChunk.updatedAt.isAfter(originalUpdatedAt))
    }

    @Test
    fun `should handle empty chunk text`() {
        val savedChunk = createChunk(chunkText = "")
        assertEquals("", savedChunk.chunkText)
    }

    private fun createChunk(
            chunkText: String = "Test text",
            chunkIndex: Int = 0,
            embedding: FloatArray = FloatArray(768) { 0.1f },
            pageStart: Int? = null,
            pageEnd: Int? = null,
            document: Document =
                    documentRepository.save(
                            Document().apply {
                                filename = "test-document.pdf"
                                status = DocumentStatus.PENDING
                            }
                    )
    ) =
            chunkRepository.save(
                    Chunk().apply {
                        this.document = document
                        this.chunkText = chunkText
                        this.chunkIndex = chunkIndex
                        this.embedding = embedding
                        this.pageStart = pageStart
                        this.pageEnd = pageEnd
                    }
            )
}
