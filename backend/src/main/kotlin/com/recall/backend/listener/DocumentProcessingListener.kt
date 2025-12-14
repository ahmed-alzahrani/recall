package com.recall.backend.listener

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import com.recall.backend.service.PDFExtractionService
import com.recall.backend.service.ChunkingService
import com.recall.backend.service.EmbeddingService
import com.recall.backend.model.DocumentStatus
import com.recall.backend.model.Document
import com.recall.backend.model.Chunk
import com.recall.backend.repository.DocumentRepository
import com.recall.backend.repository.ChunkRepository

import com.recall.backend.dto.ChunkWithEmbedding
import com.recall.backend.dto.ChunkData


@Component
class DocumentProcessingListener(
    private val pdfExtractionService: PDFExtractionService,
    private val chunkingService: ChunkingService,
    private val embeddingService: EmbeddingService,
    private val documentRepository: DocumentRepository,
    private val chunkRepository: ChunkRepository,
) {
    private val logger = LoggerFactory.getLogger(DocumentProcessingListener::class.java)

    @RabbitListener(queues = ["document.processing"])
    fun processDocument(documentId: Long) {
        val document = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found: $documentId") }
        
        document.status = DocumentStatus.PROCESSING
        documentRepository.save(document)

        try {
            val pages = pdfExtractionService.extractText(documentId)
            val chunks = chunkingService.chunkDocument(pages)
            val chunksWithEmbeddings = embeddingService.embedChunks(chunks)

            val chunkEntities = chunksWithEmbeddings.map { chunkWithEmbedding ->
                val chunk = Chunk()
                chunk.document = document
                chunk.chunkText = chunkWithEmbedding.chunkData.text
                chunk.chunkIndex = chunkWithEmbedding.chunkData.chunkIndex
                chunk.pageStart = chunkWithEmbedding.chunkData.pageStart
                chunk.pageEnd = chunkWithEmbedding.chunkData.pageEnd
                chunk.embedding = chunkWithEmbedding.embedding.toFloatArray()
                chunk
            }

            chunkRepository.saveAll(chunkEntities)

            document.status = DocumentStatus.COMPLETED
            document.totalChunks = chunkEntities.size
            documentRepository.save(document)
            
            logger.info("✅ Document processing completed successfully for document ID: $documentId")
        } catch (e: Exception) {
            logger.error("❌ Document processing failed for document ID: $documentId", e)
            document.status = DocumentStatus.FAILED
            documentRepository.save(document)
            throw e
        }
    }
}
