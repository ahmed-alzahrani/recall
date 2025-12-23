package com.recall.backend.repository

import com.recall.backend.model.Document
import com.recall.backend.model.DocumentStatus
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<Document, Long> {
    fun findByIdAndStatus(id: Long, status: DocumentStatus): Optional<Document>
}
