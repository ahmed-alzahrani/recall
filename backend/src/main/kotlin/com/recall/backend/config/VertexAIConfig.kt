package com.recall.backend.config

import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.generativeai.GenerativeModel
import javax.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VertexAIConfig(
        @Value("\${google.cloud.vertexai.project-id}") private val projectId: String,
        @Value("\${google.cloud.vertexai.location}") private val location: String,
        @Value("\${google.cloud.vertexai.gemini-model}") private val modelName: String,
) {

    private val vertexAi = VertexAI(projectId, location)

    @Bean
    fun generativeModel(): GenerativeModel {
        return GenerativeModel(modelName, vertexAi)
    }

    @PreDestroy
    fun cleanup() {
        vertexAi.close()
    }
}
