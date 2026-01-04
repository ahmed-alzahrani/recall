package com.recall.backend.service

import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.recall.backend.dto.ChunkData
import com.recall.backend.prompts.Prompts
import org.springframework.stereotype.Service

@Service
class DocumentSummaryService(
        private val generativeModel: GenerativeModel,
) {

    fun generateSummary(chunks: List<ChunkData>): String {
        val summaryChunks = sampleChunks(chunks)
        val summaryContent = summaryChunks.map { it.text }.joinToString(" ")
        val prompt = Prompts.documentSummary(summaryContent)

        val response = generativeModel.generateContent(prompt)
        return response.candidatesList.firstOrNull()?.content?.partsList?.firstOrNull()?.getText()
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
