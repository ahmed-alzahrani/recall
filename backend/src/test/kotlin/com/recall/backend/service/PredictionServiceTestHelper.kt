package com.recall.backend.service

import com.google.cloud.vertexai.api.PredictResponse
import com.google.protobuf.ListValue
import com.google.protobuf.Struct
import com.google.protobuf.Value as ProtobufValue
import org.mockito.Mockito.*

object PredictionServiceTestHelper {
    private const val EMBEDDING_DIMENSION = 768

    fun createMockPredictResponse(embeddingCount: Int): PredictResponse {
        val mockResponse = mock(PredictResponse::class.java)
        val predictions =
                (0 until embeddingCount).map { index ->
                    createPredictionValue(createMockEmbedding())
                }

        `when`(mockResponse.predictionsList).thenReturn(predictions)
        return mockResponse
    }

    private fun createPredictionValue(embedding: FloatArray): ProtobufValue {
        val valuesList =
                embedding.map { value ->
                    ProtobufValue.newBuilder().setNumberValue(value.toDouble()).build()
                }

        val embeddingsStruct =
                Struct.newBuilder()
                        .putFields(
                                "values",
                                ProtobufValue.newBuilder()
                                        .setListValue(
                                                ListValue.newBuilder()
                                                        .addAllValues(valuesList)
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()

        val predictionStruct =
                Struct.newBuilder()
                        .putFields(
                                "embeddings",
                                ProtobufValue.newBuilder().setStructValue(embeddingsStruct).build()
                        )
                        .build()

        return ProtobufValue.newBuilder().setStructValue(predictionStruct).build()
    }

    fun createMockEmbedding(): FloatArray {
        return FloatArray(EMBEDDING_DIMENSION) { it * 0.001f }
    }

    fun createMockEmbeddingWithDimension(dimension: Int): FloatArray {
        return FloatArray(dimension) { it * 0.001f }
    }

    fun createMockPredictResponseWithWrongDimension(dimension: Int): PredictResponse {
        val mockResponse = mock(PredictResponse::class.java)
        val wrongDimensionEmbedding = createMockEmbeddingWithDimension(dimension)
        val prediction = createPredictionValue(wrongDimensionEmbedding)
        `when`(mockResponse.predictionsList).thenReturn(listOf(prediction))
        return mockResponse
    }
}
