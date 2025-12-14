package com.recall.backend.dto

data class ChunkWithEmbedding(
    val chunkData: ChunkData,
    val embedding: List<Float>,
)