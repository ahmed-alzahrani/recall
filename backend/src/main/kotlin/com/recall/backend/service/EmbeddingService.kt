package com.recall.backend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.recall.backend.dto.ChunkWithEmbedding
import com.recall.backend.dto.ChunkData

import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.PredictRequest
import com.google.cloud.vertexai.api.PredictionServiceClient
import com.google.cloud.vertexai.api.PredictionServiceSettings
import com.google.cloud.vertexai.api.PredictResponse
import com.google.protobuf.Value as ProtobufValue

import com.google.protobuf.Struct


@Service
class EmbeddingService(
    @Value("\${google.cloud.vertexai.project-id}") private val projectId: String,
    @Value("\${google.cloud.vertexai.location}") private val location: String,
    @Value("\${google.cloud.vertexai.embedding-model}") private val modelName: String,
) {

    private val vertexAi = VertexAI(projectId, location)

    private val predictionServiceClient = PredictionServiceClient.create(
        PredictionServiceSettings.newBuilder()
        .setEndpoint("${location}-aiplatform.googleapis.com:443")
        .build()
    )

    fun embedChunks(chunks: List<ChunkData>): List<ChunkWithEmbedding> {
        val batchSize = 1000
        
        return chunks.chunked(batchSize).flatMap { batch ->
            val texts = batch.map { it.text }
            val instances = textToProtobufValue(texts)
            val request = predictionRequest(instances)

            val response = predictionServiceClient.predict(request)
            val embeddings = extractEmbeddings(response)

            batch.zip(embeddings).map { (chunk, embedding) ->
                ChunkWithEmbedding(
                    chunkdata = chunk,
                    embedding = embedding.toList()
                )
            }
        }
    }

    private fun textToProtobufValue(texts: List<String>): List<ProtobufValue!> {
        return texts.map { text ->
            ProtobufValue.newBuilder()
                .setStringValue(text)
                .build()
        }
    }

    private fun predictionRequest(instances: List<ProtobufValue!>): PredictRequest {
        return PredictRequest.newBuilder()
        .setEndpoint("projects/${projectId}/locations/${location}/publishers/google/models/${modelName}")
        .addAllInstances(instances)
        .setParameters(
            ProtobufValue.newBuilder()
                .setStructValue(
                    Struct.newBuilder()
                        .putFields("taskType", ProtobufValue.newBuilder().setStringValue("RETRIEVAL_DOCUMENT").build())
                        .build()
                )
                .build()
        )
        .build()
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
    
}