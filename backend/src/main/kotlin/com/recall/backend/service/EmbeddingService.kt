package com.recall.backend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.recall.backend.dto.ChunkWithEmbedding
import com.recall.backend.dto.ChunkData

import com.google.cloud.vertexai.api.PredictionServiceClient
import com.google.cloud.vertexai.api.PredictionServiceSettings
import com.google.cloud.vertexai.api.PredictResponse
import com.google.protobuf.Value as ProtobufValue

import com.google.protobuf.Struct
import javax.annotation.PreDestroy


@Service
class EmbeddingService(
    @Value("\${google.cloud.vertexai.project-id}") private val projectId: String,
    @Value("\${google.cloud.vertexai.location}") private val location: String,
    @Value("\${google.cloud.vertexai.embedding-model}") private val modelName: String,
) {

    companion object {
        private const val BATCH_SIZE = 1000;
        private const val EMBEDDING_DIMENSION = 768;
    }


    private val predictionServiceClient = PredictionServiceClient.create(
        PredictionServiceSettings.newBuilder()
        .setEndpoint("${location}-aiplatform.googleapis.com:443")
        .build()
    )

    @PreDestroy
    fun cleanup() {
        predictionServiceClient.close()
    }

    fun embedChunks(chunks: List<ChunkData>): List<ChunkWithEmbedding> {
        val modelPath = "projects/${projectId}/locations/${location}/publishers/google/models/${modelName}"
        
        return chunks.chunked(BATCH_SIZE).flatMap { batch ->
            try {
                val texts = batch.map { it.text }
                val instances = textToProtobufValue(texts)
                val parameters = ProtobufValue.newBuilder()
                    .setStructValue(
                        Struct.newBuilder()
                            .putFields("taskType", ProtobufValue.newBuilder().setStringValue("RETRIEVAL_DOCUMENT").build())
                            .build()
                    )
                    .build()
    
                val response = predictionServiceClient.predict(modelPath, instances, parameters)
                val embeddings = extractEmbeddings(response)

                validateEmbeddings(batch, embeddings)
    
                batch.zip(embeddings).map { (chunk, embedding) ->
                    ChunkWithEmbedding(
                        chunkData = chunk,
                        embedding = embedding.toList()
                    )
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed to embed batch of ${batch.size} chunks: ${e.message}", e)
            }
        }
    }

    private fun textToProtobufValue(texts: List<String>): List<ProtobufValue> {
        return texts.map { text ->
            ProtobufValue.newBuilder()
                .setStructValue(
                    Struct.newBuilder()
                        .putFields("content", ProtobufValue.newBuilder().setStringValue(text).build())
                        .build()
                )
                .build()
        }
    }

    private fun extractEmbeddings(response: PredictResponse): List<FloatArray> {
        return response.predictionsList.map { prediction ->
            val struct = prediction.structValue
            val embeddingsField = struct?.fieldsMap?.get("embeddings")
            val embeddingsStruct = embeddingsField?.structValue
            val valuesField = embeddingsStruct?.fieldsMap?.get("values")
            val valuesList = valuesField?.listValue?.valuesList ?: emptyList()
            
            // Convert protobuf values to FloatArray
            valuesList.mapNotNull { value ->
                if (value.hasNumberValue()) {
                    value.numberValue.toFloat()
                } else {
                    null
                }
            }.toFloatArray()
        }
    }

    private fun validateEmbeddings(batch: List<ChunkData>, embeddings: List<FloatArray>) {
        if (batch.size != embeddings.size) {
            throw RuntimeException("Expected ${batch.size} embeddings, got ${embeddings.size}")
        }
        
        embeddings.forEachIndexed { index, embedding ->
            if (embedding.size != EMBEDDING_DIMENSION) {
                throw RuntimeException("Embedding at index $index has incorrect dimension: ${embedding.size}, expected $EMBEDDING_DIMENSION")
            }
        }
    }
    
}