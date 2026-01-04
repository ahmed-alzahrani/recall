package com.recall.backend.service

import com.google.cloud.vertexai.generativeai.GenerativeModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class AnswerServiceTest {

    private lateinit var mockGenerativeModel: GenerativeModel
    private lateinit var answerService: AnswerService

    @BeforeEach
    fun setUp() {
        mockGenerativeModel = mock(GenerativeModel::class.java)
        answerService = AnswerService(mockGenerativeModel)
    }

    @Test
    fun `should return answer when generative model returns valid response`() {
        val question = "What is the main topic?"
        val chunkTexts = listOf("Chunk 1 text", "Chunk 2 text")
        val expectedAnswer = "The main topic is about testing."

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, text = expectedAnswer)

        val result = answerService.answerQuestion(question, chunkTexts)
        assertEquals(expectedAnswer, result)
        verify(mockGenerativeModel).generateContent(anyString())
    }

    @Test
    fun `should throw exception when response has no candidates`() {

        val question = "What is the main topic?"
        val chunkTexts = listOf("Chunk 1 text")

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, candidates = emptyList())

        assertThrows(IllegalStateException::class.java) {
            answerService.answerQuestion(question, chunkTexts)
        }
    }

    @Test
    fun `should throw exception when candidate has no content`() {
        val question = "What is the main topic?"
        val chunkTexts = listOf("Chunk 1 text")

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, content = null)

        assertThrows(IllegalStateException::class.java) {
            answerService.answerQuestion(question, chunkTexts)
        }
    }

    @Test
    fun `should throw exception when content has no parts`() {
        val question = "What is the main topic?"
        val chunkTexts = listOf("Chunk 1 text")

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, parts = emptyList())

        assertThrows(IllegalStateException::class.java) {
            answerService.answerQuestion(question, chunkTexts)
        }
    }

    @Test
    fun `should return empty string when part text is empty`() {
        val question = "What is the main topic?"
        val chunkTexts = listOf("Chunk 1 text")

        GenerativeModelTestHelper.setupMockResponse(mockGenerativeModel, text = "")

        val result = answerService.answerQuestion(question, chunkTexts)

        assertEquals("", result)
    }
}
