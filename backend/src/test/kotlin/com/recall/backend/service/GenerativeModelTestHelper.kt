package com.recall.backend.service

import com.google.cloud.vertexai.api.Candidate
import com.google.cloud.vertexai.api.Content
import com.google.cloud.vertexai.api.GenerateContentResponse
import com.google.cloud.vertexai.api.Part
import com.google.cloud.vertexai.generativeai.GenerativeModel
import org.mockito.Mockito.*

object GenerativeModelTestHelper {
    fun setupMockResponse(
            generativeModel: GenerativeModel,
            candidates: List<Candidate>? = null,
            content: Content? = null,
            parts: List<Part>? = null,
            text: String? = null
    ): GenerateContentResponse {
        val mockResponse = mock(GenerateContentResponse::class.java)
        `when`(generativeModel.generateContent(anyString())).thenReturn(mockResponse)

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
