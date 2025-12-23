package com.recall.backend.service

import com.google.cloud.vertexai.api.PredictResponse
import com.google.cloud.vertexai.api.PredictionServiceClient
import com.google.cloud.vertexai.api.PredictionServiceSettings
import com.google.protobuf.Struct
import com.google.protobuf.Value as ProtobufValue
import com.recall.backend.dto.ChunkData
import com.recall.backend.dto.ChunkWithEmbedding
import javax.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class EmbeddingService(
        @Value("\${google.cloud.vertexai.project-id}") private val projectId: String,
        @Value("\${google.cloud.vertexai.location}") private val location: String,
        @Value("\${google.cloud.vertexai.embedding-model}") private val modelName: String,
) {

    companion object {
        private const val BATCH_SIZE = 1000
        private const val EMBEDDING_DIMENSION = 768
    }

    private val predictionServiceClient =
            PredictionServiceClient.create(
                    PredictionServiceSettings.newBuilder()
                            .setEndpoint("${location}-aiplatform.googleapis.com:443")
                            .build()
            )

    private val modelPath =
            "projects/${projectId}/locations/${location}/publishers/google/models/${modelName}"

    @PreDestroy
    fun cleanup() {
        predictionServiceClient.close()
    }

    fun embedChunks(chunks: List<ChunkData>): List<ChunkWithEmbedding> {
        return chunks.chunked(BATCH_SIZE).flatMap { batch ->
            try {
                val embeddings = callEmbeddingApi(batch.map { it.text }, "RETRIEVAL_DOCUMENT")
                require(batch.size == embeddings.size) {
                    "Expected ${batch.size} embeddings, got ${embeddings.size}"
                }
                validateEmbeddings(embeddings)
                batch.zip(embeddings).map { (chunk, embedding) ->
                    ChunkWithEmbedding(chunkData = chunk, embedding = embedding.toList())
                }
            } catch (e: Exception) {
                throw RuntimeException(
                        "Failed to embed batch of ${batch.size} chunks: ${e.message}",
                        e
                )
            }
        }
    }

    fun embedQuestion(question: String): FloatArray {
        try {
            val embeddings = callEmbeddingApi(listOf(question), "RETRIEVAL_QUERY")
            require(embeddings.isNotEmpty()) { "No embedding returned for question" }
            validateEmbeddings(embeddings)
            return embeddings.first()
        } catch (e: Exception) {
            throw RuntimeException("Failed to embed question: ${e.message}", e)
        }
    }

    private fun callEmbeddingApi(texts: List<String>, taskType: String): List<FloatArray> {
        val parameters = createStructValue("taskType", taskType)
        val response =
                predictionServiceClient.predict(modelPath, textToProtobufValue(texts), parameters)
        return extractEmbeddings(response)
    }

    private fun textToProtobufValue(texts: List<String>): List<ProtobufValue> {
        return texts.map { createStructValue("content", it) }
    }

    private fun createStructValue(key: String, value: String): ProtobufValue {
        return ProtobufValue.newBuilder()
                .setStructValue(
                        Struct.newBuilder()
                                .putFields(
                                        key,
                                        ProtobufValue.newBuilder().setStringValue(value).build()
                                )
                                .build()
                )
                .build()
    }

    private fun extractEmbeddings(response: PredictResponse): List<FloatArray> {
        return response.predictionsList.map { prediction ->
            val valuesList =
                    prediction
                            .structValue
                            ?.fieldsMap
                            ?.get("embeddings")
                            ?.structValue
                            ?.fieldsMap
                            ?.get("values")
                            ?.listValue
                            ?.valuesList
                            ?: emptyList()
            valuesList
                    .mapNotNull { if (it.hasNumberValue()) it.numberValue.toFloat() else null }
                    .toFloatArray()
        }
    }

    private fun validateEmbeddings(embeddings: List<FloatArray>) {
        embeddings.forEachIndexed { index, embedding ->
            require(embedding.size == EMBEDDING_DIMENSION) {
                "Embedding at index $index has incorrect dimension: ${embedding.size}, expected $EMBEDDING_DIMENSION"
            }
        }
    }
}
