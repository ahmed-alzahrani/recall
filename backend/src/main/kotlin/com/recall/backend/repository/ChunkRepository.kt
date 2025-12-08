package com.recall.backend.repository

import com.recall.backend.model.Chunk
import org.springframework.data.jpa.repository.JpaRepository

interface ChunkRepository : JpaRepository<Chunk, Long>

