package com.recall.backend.service

import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.recall.backend.prompts.Prompts
import javax.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AnswerService(
        @Value("\${google.cloud.vertexai.project-id}") private val projectId: String,
        @Value("\${google.cloud.vertexai.location}") private val location: String,
        @Value("\${google.cloud.vertexai.gemini-model}") private val modelName: String,
) {

    private val vertexAi = VertexAI(projectId, location)
    private val generativeModel = GenerativeModel(modelName, vertexAi)

    @PreDestroy
    fun cleanup() {
        vertexAi.close()
    }

    fun answerQuestion(question: String, chunkTexts: List<String>): String {
        val prompt = Prompts.answerQuestion(question, chunkTexts)

        val response = generativeModel.generateContent(prompt)
        return response.candidatesList.firstOrNull()?.content?.partsList?.firstOrNull()?.getText()
                ?: throw IllegalStateException("No response generated from Gemini")
    }
}
