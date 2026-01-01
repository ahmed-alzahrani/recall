package com.recall.backend.controller

import com.recall.backend.model.Document
import com.recall.backend.model.DocumentStatus
import com.recall.backend.repository.ChunkRepository
import com.recall.backend.repository.DocumentRepository
import com.recall.backend.service.AnswerService
import com.recall.backend.service.EmbeddingService
import kotlin.io.path.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = ["http://localhost:3000"])
class DocumentController(
        private val documentRepository: DocumentRepository,
        private val rabbitTemplate: RabbitTemplate,
        private val embeddingService: EmbeddingService,
        private val chunkRepository: ChunkRepository,
        private val answerService: AnswerService,
        @Value("\${app.upload.tmp-dir}") private val tmpFileStoragePath: String,
) {

    private val TMP_FILE_STORAGE = Path(tmpFileStoragePath)

    @PostMapping("/upload")
    fun uploadDocument(@RequestParam("file") file: MultipartFile): Map<String, Any?> {

        if (file.contentType != "application/pdf") {
            return mapOf("error" to "Only PDF files are allowed")
        }

        val maxSize = 65 * 1024 * 1024 // 65MB
        if (file.size > maxSize) {
            return mapOf("error" to "File size exceeds 65MB limit")
        }

        val savedDocument =
                documentRepository.save(
                        Document().apply {
                            filename = file.originalFilename ?: "unknown.pdf"
                            status = DocumentStatus.PENDING
                        }
                )

        val filePath = TMP_FILE_STORAGE.resolve(savedDocument.id.toString())
        file.transferTo(filePath)

        rabbitTemplate.convertAndSend(
                "document.processing",
                checkNotNull(savedDocument.id) { "Document ID should be populated after save" }
        )

        return mapOf(
                "message" to "File uploaded successfully",
                "filename" to file.originalFilename,
                "size" to file.size,
                "documentId" to savedDocument.id
        )
    }

    @GetMapping
    fun getDocuments(): List<Map<String, Any?>> {
        return documentRepository.findAll().map { document ->
            mapOf(
                    "documentId" to document.id,
                    "filename" to document.filename,
                    "status" to document.status.name,
                    "summary" to document.summary,
                    "totalChunks" to document.totalChunks,
                    "createdAt" to document.createdAt,
                    "updatedAt" to document.updatedAt
            )
        }
    }

    @GetMapping("/{documentId}/status")
    fun getDocumentStatus(@PathVariable documentId: Long): Map<String, Any?> {
        val document =
                documentRepository.findById(documentId).orElseThrow {
                    RuntimeException("Document not found with id: $documentId")
                }

        return mapOf(
                "documentId" to document.id,
                "status" to document.status.name,
                "filename" to document.filename,
                "totalChunks" to document.totalChunks,
                "createdAt" to document.createdAt,
                "updatedAt" to document.updatedAt
        )
    }

    @GetMapping("/{documentId}")
    fun getDocument(@PathVariable documentId: Long): Map<String, Any?> {
        val document =
                documentRepository.findById(documentId).orElseThrow {
                    RuntimeException("Document not found with id: $documentId")
                }

        return mapOf(
                "documentId" to document.id,
                "filename" to document.filename,
                "status" to document.status.name,
                "summary" to document.summary,
                "totalChunks" to document.totalChunks,
                "createdAt" to document.createdAt,
                "updatedAt" to document.updatedAt
        )
    }

    @PostMapping("/{documentId}/chat")
    fun chat(@PathVariable documentId: Long, @RequestBody question: String): Map<String, Any?> {
        val maxQuestionLength = 1000
        if (question.length > maxQuestionLength) {
            return mapOf(
                    "error" to "Question exceeds maximum length of $maxQuestionLength characters"
            )
        }

        documentRepository.findByIdAndStatus(documentId, DocumentStatus.COMPLETED).orElseThrow {
            RuntimeException("No completed document found with id: $documentId")
        }

        val embeddedQuestion = embeddingService.embedQuestion(question)

        val relevantChunks = chunkRepository.findSimilarChunks(documentId, embeddedQuestion)

        val chunkTexts = relevantChunks.map { it.chunkText }

        val answer = answerService.answerQuestion(question, chunkTexts)

        return mapOf("answer" to answer)
    }
}
