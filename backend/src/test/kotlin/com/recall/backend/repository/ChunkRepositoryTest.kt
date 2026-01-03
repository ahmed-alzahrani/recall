package com.recall.backend.repository

import com.recall.backend.annotations.DatabaseTest
import com.recall.backend.model.Chunk
import com.recall.backend.model.Document
import com.recall.backend.model.DocumentStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DatabaseTest
class ChunkRepositoryTest {
    @Autowired lateinit var chunkRepository: ChunkRepository
    @Autowired lateinit var documentRepository: DocumentRepository

    @Test
    fun `should find 5 most similar chunks`() {
        val document =
                documentRepository.save(
                        Document().apply {
                            filename = "test.pdf"
                            status = DocumentStatus.PENDING
                        }
                )

        // embed the query as if the first elemnt is 1.0 and the rest of the embedding is 0.0
        val queryEmbedding = FloatArray(768) { if (it == 0) 1.0f else 0.0f }
        // create chunks of decreasing similarity, the first chunk is 1.0 and 0.0 for the rest
        // the second is 0.9 and 0.0 for the rest, etc. until the last chunk leads with 0.1
        val chunks =
                (0..9).map { i ->
                    val embedding = FloatArray(768) { if (it == 0) (1.0f - i * 0.1f) else 0.0f }
                    chunkRepository.save(createChunk(document, embedding, i))
                }

        val results = chunkRepository.findSimilarChunks(document.id!!, queryEmbedding)

        assertEquals(5, results.size)
        assertEquals(chunks[0].id, results[0].id)
        assertEquals(chunks[1].id, results[1].id)
    }

    @Test
    fun `should only return chunks from specified document`() {
        val doc1 =
                documentRepository.save(
                        Document().apply {
                            filename = "doc1.pdf"
                            status = DocumentStatus.PENDING
                        }
                )
        val doc2 =
                documentRepository.save(
                        Document().apply {
                            filename = "doc2.pdf"
                            status = DocumentStatus.PENDING
                        }
                )

        val embedding = FloatArray(768) { 0.1f }
        chunkRepository.save(createChunk(doc1, embedding, 0))
        chunkRepository.save(createChunk(doc2, embedding, 0))

        val results = chunkRepository.findSimilarChunks(doc1.id!!, embedding)

        assertEquals(1, results.size)
        assertEquals(doc1.id, results[0].document?.id)
    }

    @Test
    fun `should return fewer than 5 when document has fewer chunks`() {
        val document =
                documentRepository.save(
                        Document().apply {
                            filename = "test.pdf"
                            status = DocumentStatus.PENDING
                        }
                )

        val embedding = FloatArray(768) { 0.1f }
        chunkRepository.save(createChunk(document, embedding, 0))
        chunkRepository.save(createChunk(document, embedding, 1))

        val results = chunkRepository.findSimilarChunks(document.id!!, embedding)

        assertEquals(2, results.size)
    }

    private fun createChunk(document: Document, embedding: FloatArray, index: Int) =
            Chunk().apply {
                this.document = document
                chunkText = "Chunk $index"
                chunkIndex = index
                this.embedding = embedding
            }
}
