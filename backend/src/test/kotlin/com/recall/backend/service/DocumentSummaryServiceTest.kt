package com.recall.backend.service

import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.recall.backend.dto.ChunkData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class DocumentSummaryServiceTest {

    private lateinit var mockGenerativeModel: GenerativeModel
    private lateinit var documentSummaryService: DocumentSummaryService

    @BeforeEach
    fun setUp() {
        mockGenerativeModel = mock(GenerativeModel::class.java)
        documentSummaryService = DocumentSummaryService(mockGenerativeModel)
    }

    @Test
    fun `should return summary when generative model returns valid response`() {
        val chunks =
                listOf(
                        ChunkData("First chunk text", 0, 1, 1),
                        ChunkData("Second chunk text", 1, 1, 1)
                )
        val expectedSummary = "This is a summary of the document."

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, text = expectedSummary)

        val result = documentSummaryService.generateSummary(chunks)

        assertEquals(expectedSummary, result)
        verify(mockGenerativeModel).generateContent(anyString())
    }

    @Test
    fun `should throw exception when response has no candidates`() {
        val chunks = listOf(ChunkData("Chunk text", 0, 1, 1))

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, candidates = emptyList())

        assertThrows(IllegalStateException::class.java) {
            documentSummaryService.generateSummary(chunks)
        }
    }

    @Test
    fun `should throw exception when candidate has no content`() {
        val chunks = listOf(ChunkData("Chunk text", 0, 1, 1))

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, content = null)

        assertThrows(IllegalStateException::class.java) {
            documentSummaryService.generateSummary(chunks)
        }
    }

    @Test
    fun `should throw exception when content has no parts`() {
        val chunks = listOf(ChunkData("Chunk text", 0, 1, 1))

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, parts = emptyList())

        assertThrows(IllegalStateException::class.java) {
            documentSummaryService.generateSummary(chunks)
        }
    }

    @Test
    fun `should use all chunks when chunk count is 15 or less`() {
        val chunks = (1..15).map { ChunkData("Chunk $it", it - 1, 1, 1) }
        val expectedSummary = "Summary"

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, text = expectedSummary)

        val result = documentSummaryService.generateSummary(chunks)

        assertEquals(expectedSummary, result)
        verify(mockGenerativeModel).generateContent(anyString())
    }

    @Test
    fun `should sample chunks when chunk count is greater than 15`() {
        val chunks = (1..20).map { ChunkData("Chunk $it", it - 1, 1, 1) }
        val expectedSummary = "Summary"

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, text = expectedSummary)

        val result = documentSummaryService.generateSummary(chunks)

        assertEquals(expectedSummary, result)
        verify(mockGenerativeModel).generateContent(anyString())
    }

    @Test
    fun `should call generateContent when chunk count is large`() {
        val chunks = (1..50).map { ChunkData("Chunk $it", it - 1, 1, 1) }
        val expectedSummary = "Summary"

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, text = expectedSummary)

        val result = documentSummaryService.generateSummary(chunks)

        assertEquals(expectedSummary, result)
        verify(mockGenerativeModel).generateContent(anyString())
    }
}
