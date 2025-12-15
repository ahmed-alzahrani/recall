package com.recall.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "documents")
class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false, length = 255)
    var filename: String = ""
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DocumentStatus = DocumentStatus.PENDING

    @Column(name = "total_chunks")
    var totalChunks: Int? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()

    @Column(nullable = true, length = 500)
    var summary: String? = null

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var chunks: MutableList<Chunk> = mutableListOf()

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

