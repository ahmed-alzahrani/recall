package com.recall.backend.controller

import kotlin.io.path.*

import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

import com.recall.backend.repository.DocumentRepository
import com.recall.backend.model.DocumentStatus
import com.recall.backend.model.Document
import com.recall.backend.config.RabbitMQConfig

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = ["http://localhost:3000"])
class DocumentController(
    private val documentRepository: DocumentRepository,
) {

    private val TMP_FILE_STORAGE = Path("../.uploads/tmp")
    
    @PostMapping("/upload")
    fun uploadDocument(@RequestParam("file") file: MultipartFile): Map<String, Any?> {

        if (file.contentType != "application/pdf") {
            return mapOf("error" to "Only PDF files are allowed")
        }

        val maxSize = 25 * 1024 * 1024 // 25MB
        if (file.size > maxSize) {
            return mapOf("error" to "File size exceeds 25MB limit")
        }

        val savedDocument = documentRepository.save(Document().apply {
            filename = file.originalFilename ?: "unknown.pdf"
            status = DocumentStatus.PENDING
        })

        val filePath = TMP_FILE_STORAGE.resolve(savedDocument.id.toString())
        file.transferTo(filePath)


        return mapOf(
            "message" to "File uploaded successfully",
            "filename" to file.originalFilename,
            "size" to file.size
            )
     
        
    }
}