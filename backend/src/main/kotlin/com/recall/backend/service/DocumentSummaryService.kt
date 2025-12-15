package com.recall.backend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.recall.backend.dto.ChunkData
import com.recall.backend.prompts.Prompts
import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.generativeai.GenerativeModel
import javax.annotation.PreDestroy

@Service
class DocumentSummaryService(
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

    fun generateSummary(chunks: List<ChunkData>): String {
        val summaryChunks = sampleChunks(chunks)
        val summaryContent = summaryChunks.map { it.text }.joinToString(" ")
        val prompt = Prompts.documentSummary(summaryContent)
        
        val response = generativeModel.generateContent(prompt)
        return response.candidatesList
            .firstOrNull()
            ?.content
            ?.partsList
            ?.firstOrNull()
            ?.getText()
            ?: throw IllegalStateException("No response generated from Gemini")
    }

    private fun sampleChunks(chunks: List<ChunkData>): List<ChunkData> {
        if (chunks.size <= 15) return chunks
        
        val middleIndex = chunks.size / 2
        val middleStart = maxOf(5, middleIndex - 2)
        val middleEnd = minOf(chunks.size - 5, middleStart + 5)
        
        return chunks.take(5) + chunks.slice(middleStart until middleEnd) + chunks.takeLast(5)
    }
}