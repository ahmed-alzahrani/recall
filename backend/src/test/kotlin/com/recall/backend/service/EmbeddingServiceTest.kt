package com.recall.backend.service

import com.google.cloud.vertexai.api.PredictionServiceClient
import com.google.protobuf.Value as ProtobufValue
import com.recall.backend.dto.ChunkData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class EmbeddingServiceTest {

    private lateinit var mockPredictionServiceClient: PredictionServiceClient
    private lateinit var embeddingService: EmbeddingService

    companion object {
        private const val EMBEDDING_DIMENSION = 768
        private const val BATCH_SIZE = 15
    }

    @BeforeEach
    fun setUp() {
        mockPredictionServiceClient = mock(PredictionServiceClient::class.java)
        embeddingService =
                EmbeddingService(
                        mockPredictionServiceClient,
                        "test-project",
                        "us-central1",
                        "textembedding-gecko@003"
                )
    }

    @Test
    fun `should embed single chunk successfully`() {
        val chunk = ChunkData("Test chunk text", 0, 1, 1)
        val mockResponse = PredictionServiceTestHelper.createMockPredictResponse(1)

        `when`(
                        mockPredictionServiceClient.predict(
                                anyString(),
                                anyList(),
                                any(ProtobufValue::class.java)
                        )
                )
                .thenReturn(mockResponse)

        val result = embeddingService.embedChunks(listOf(chunk))

        assertEquals(1, result.size)
        assertEquals(chunk, result[0].chunkData)
        assertEquals(EMBEDDING_DIMENSION, result[0].embedding.size)
    }

    @Test
    fun `should embed multiple chunks in single batch`() {
        val chunks = (1..5).map { ChunkData("Chunk $it", it - 1, 1, 1) }
        val mockResponse = PredictionServiceTestHelper.createMockPredictResponse(5)

        `when`(
                        mockPredictionServiceClient.predict(
                                anyString(),
                                anyList(),
                                any(ProtobufValue::class.java)
                        )
                )
                .thenReturn(mockResponse)

        val result = embeddingService.embedChunks(chunks)

        assertEquals(5, result.size)
        result.forEachIndexed { index, chunkWithEmbedding ->
            assertEquals(chunks[index], chunkWithEmbedding.chunkData)
            assertEquals(EMBEDDING_DIMENSION, chunkWithEmbedding.embedding.size)
        }
    }

    @Test
    fun `should process chunks in batches when exceeding batch size`() {
        val chunks = (1..20).map { ChunkData("Chunk $it", it - 1, 1, 1) }
        val firstBatchResponse = PredictionServiceTestHelper.createMockPredictResponse(BATCH_SIZE)
        val secondBatchResponse = PredictionServiceTestHelper.createMockPredictResponse(5)

        `when`(
                        mockPredictionServiceClient.predict(
                                anyString(),
                                anyList(),
                                any(ProtobufValue::class.java)
                        )
                )
                .thenReturn(firstBatchResponse, secondBatchResponse)

        val result = embeddingService.embedChunks(chunks)

        assertEquals(20, result.size)
        verify(mockPredictionServiceClient, times(2))
                .predict(anyString(), anyList(), any(ProtobufValue::class.java))
    }

    @Test
    fun `should embed question successfully`() {
        val question = "What is the main topic?"
        val mockResponse = PredictionServiceTestHelper.createMockPredictResponse(1)

        `when`(
                        mockPredictionServiceClient.predict(
                                anyString(),
                                anyList(),
                                any(ProtobufValue::class.java)
                        )
                )
                .thenReturn(mockResponse)

        val result = embeddingService.embedQuestion(question)

        assertEquals(EMBEDDING_DIMENSION, result.size)
        verify(mockPredictionServiceClient)
                .predict(anyString(), anyList(), any(ProtobufValue::class.java))
    }

    @Test
    fun `should throw exception when embedding count mismatch`() {
        val chunks = listOf(ChunkData("Chunk 1", 0, 1, 1), ChunkData("Chunk 2", 1, 1, 1))
        // Return only 1 embedding instead of 2
        val mockResponse = PredictionServiceTestHelper.createMockPredictResponse(1)

        `when`(
                        mockPredictionServiceClient.predict(
                                anyString(),
                                anyList(),
                                any(ProtobufValue::class.java)
                        )
                )
                .thenReturn(mockResponse)

        assertThrows(RuntimeException::class.java) { embeddingService.embedChunks(chunks) }
    }

    @Test
    fun `should throw exception when no embeddings returned for question`() {
        val question = "Test question"
        val mockResponse = PredictionServiceTestHelper.createMockPredictResponse(0)

        `when`(
                        mockPredictionServiceClient.predict(
                                anyString(),
                                anyList(),
                                any(ProtobufValue::class.java)
                        )
                )
                .thenReturn(mockResponse)

        assertThrows(RuntimeException::class.java) { embeddingService.embedQuestion(question) }
    }

    @Test
    fun `should throw exception when embedding has wrong dimension`() {
        val chunk = ChunkData("Test chunk", 0, 1, 1)
        val mockResponse =
                PredictionServiceTestHelper.createMockPredictResponseWithWrongDimension(767)

        `when`(
                        mockPredictionServiceClient.predict(
                                anyString(),
                                anyList(),
                                any(ProtobufValue::class.java)
                        )
                )
                .thenReturn(mockResponse)

        assertThrows(RuntimeException::class.java) { embeddingService.embedChunks(listOf(chunk)) }
    }

    @Test
    fun `should handle empty chunks list`() {
        val result = embeddingService.embedChunks(emptyList())

        assertTrue(result.isEmpty())
        verify(mockPredictionServiceClient, never())
                .predict(anyString(), anyList(), any(ProtobufValue::class.java))
    }

    @Test
    fun `should wrap API exceptions in RuntimeException`() {
        val chunk = ChunkData("Test chunk", 0, 1, 1)

        `when`(
                        mockPredictionServiceClient.predict(
                                anyString(),
                                anyList(),
                                any(ProtobufValue::class.java)
                        )
                )
                .thenThrow(RuntimeException("API error"))

        assertThrows(RuntimeException::class.java) { embeddingService.embedChunks(listOf(chunk)) }
    }
}
