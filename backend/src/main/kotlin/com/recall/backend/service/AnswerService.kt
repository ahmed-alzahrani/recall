package com.recall.backend.service

import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.recall.backend.prompts.Prompts
import org.springframework.stereotype.Service

@Service
class AnswerService(
        private val generativeModel: GenerativeModel,
) {

    fun answerQuestion(question: String, chunkTexts: List<String>): String {
        val prompt = Prompts.answerQuestion(question, chunkTexts)

        val response = generativeModel.generateContent(prompt)
        return response.candidatesList.firstOrNull()?.content?.partsList?.firstOrNull()?.getText()
                ?: throw IllegalStateException("No response generated from Gemini")
    }
}
