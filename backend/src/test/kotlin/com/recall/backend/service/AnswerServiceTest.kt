package com.recall.backend.service

import com.google.cloud.vertexai.api.Candidate
import com.google.cloud.vertexai.api.Content
import com.google.cloud.vertexai.api.GenerateContentResponse
import com.google.cloud.vertexai.api.Part
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

        setupMockResponse(text = expectedAnswer)

        val result = answerService.answerQuestion(question, chunkTexts)
        assertEquals(expectedAnswer, result)
        verify(mockGenerativeModel).generateContent(anyString())
    }

    @Test
    fun `should throw exception when response has no candidates`() {

        val question = "What is the main topic?"
        val chunkTexts = listOf("Chunk 1 text")

        setupMockResponse(candidates = emptyList())

        assertThrows(IllegalStateException::class.java) {
            answerService.answerQuestion(question, chunkTexts)
        }
    }

    @Test
    fun `should throw exception when candidate has no content`() {
        val question = "What is the main topic?"
        val chunkTexts = listOf("Chunk 1 text")

        setupMockResponse(content = null)

        assertThrows(IllegalStateException::class.java) {
            answerService.answerQuestion(question, chunkTexts)
        }
    }

    @Test
    fun `should throw exception when content has no parts`() {
        val question = "What is the main topic?"
        val chunkTexts = listOf("Chunk 1 text")

        setupMockResponse(parts = emptyList())

        assertThrows(IllegalStateException::class.java) {
            answerService.answerQuestion(question, chunkTexts)
        }
    }

    @Test
    fun `should return empty string when part text is empty`() {
        val question = "What is the main topic?"
        val chunkTexts = listOf("Chunk 1 text")

        setupMockResponse(text = "")

        val result = answerService.answerQuestion(question, chunkTexts)

        assertEquals("", result)
    }

    private fun setupMockResponse(
            candidates: List<Candidate>? = null,
            content: Content? = null,
            parts: List<Part>? = null,
            text: String? = null
    ): GenerateContentResponse {
        val mockResponse = mock(GenerateContentResponse::class.java)
        `when`(mockGenerativeModel.generateContent(anyString())).thenReturn(mockResponse)

        val candidatesToReturn =
                if (candidates != null) {
                    candidates
                } else {
                    val mockCandidate = mock(Candidate::class.java)
                    val contentToReturn =
                            if (content != null) {
                                content
                            } else {
                                val mockContent = mock(Content::class.java)
                                val partsToReturn =
                                        if (parts != null) {
                                            parts
                                        } else {
                                            val mockPart = mock(Part::class.java)
                                            text?.let { `when`(mockPart.text).thenReturn(it) }
                                            listOf(mockPart)
                                        }
                                `when`(mockContent.partsList).thenReturn(partsToReturn)
                                mockContent
                            }
                    `when`(mockCandidate.content).thenReturn(contentToReturn)
                    listOf(mockCandidate)
                }

        `when`(mockResponse.candidatesList).thenReturn(candidatesToReturn)
        return mockResponse
    }
}
