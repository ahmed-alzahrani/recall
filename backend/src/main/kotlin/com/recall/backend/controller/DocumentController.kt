package com.recall.backend.controller

import com.recall.backend.model.Document
import com.recall.backend.model.DocumentStatus
import com.recall.backend.repository.DocumentRepository
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
        @Value("\${app.upload.tmp-dir}") private val tmpFileStoragePath: String,
) {

    private val TMP_FILE_STORAGE = Path(tmpFileStoragePath)
    
    @PostMapping("/upload")
    fun uploadDocument(@RequestParam("file") file: MultipartFile): Map<String, Any?> {

        if (file.contentType != "application/pdf") {
            return mapOf("error" to "Only PDF files are allowed")
        }

        val maxSize = 25 * 1024 * 1024 // 25MB
        if (file.size > maxSize) {
            return mapOf("error" to "File size exceeds 25MB limit")
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

        // we need to return the document id to the frontend so it can poll for the status
        // after this, we need to send a message to RabbitMQ to start the processing

        // Beyond this, we need the infrastructure to listen to the message and process the PDF into
        // chunks

        return mapOf(
            "message" to "File uploaded successfully",
            "filename" to file.originalFilename,
            "size" to file.size,
            "documentId" to savedDocument.id
            )
    }
}
