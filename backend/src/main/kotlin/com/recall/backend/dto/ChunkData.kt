package com.recall.backend.dto

data class ChunkData(
    val text: String,
    val chunkIndex: Int,
    val pageStart: Int,
    val pageEnd: Int,
)