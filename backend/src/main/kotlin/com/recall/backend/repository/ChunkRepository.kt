package com.recall.backend.repository

import com.recall.backend.model.Chunk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChunkRepository : JpaRepository<Chunk, Long> {
        @Query(
                value =
                        "SELECT * FROM chunks WHERE document_id = :documentId ORDER BY embedding <=> CAST(:queryEmbedding AS vector) LIMIT 5",
                nativeQuery = true
        )
        fun findSimilarChunks(
                @Param("documentId") documentId: Long,
                @Param("queryEmbedding") queryEmbedding: FloatArray
        ): List<Chunk>
}
