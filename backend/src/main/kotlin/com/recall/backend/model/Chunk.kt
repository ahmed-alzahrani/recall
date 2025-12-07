package com.recall.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "chunks")
class Chunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    var document: Document? = null

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    var chunkText: String = ""

    @Column(name = "chunk_index", nullable = false)
    var chunkIndex: Int = 0

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(768)")
    var embedding: FloatArray? = null

    @Column(name = "page_start")
    var pageStart: Int? = null

    @Column(name = "page_end")
    var pageEnd: Int? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()

    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

