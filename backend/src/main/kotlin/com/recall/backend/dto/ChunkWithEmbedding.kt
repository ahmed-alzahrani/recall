package com.recall.backend.dto

data class ChunkWithEmbedding(
    val chunkdata: ChunkData,
    val embedding: List<Float>,
)