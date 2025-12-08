package com.recall.backend.repository

import com.recall.backend.model.Document
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<Document, Long>

