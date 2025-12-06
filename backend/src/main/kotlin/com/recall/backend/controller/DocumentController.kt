package com.recall.backend.controller

import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = ["http://localhost:3000"])
class DocumentController {
    
    @PostMapping("/upload")
    fun uploadDocument(@RequestParam("file") file: MultipartFile): Map<String, Any?> {

        if (file.contentType != "application/pdf") {
            return mapOf("error" to "Only PDF files are allowed")
        }

        val maxSize = 25 * 1024 * 1024 // 25MB
        if (file.size > maxSize) {
            return mapOf("error" to "File size exceeds 25MB limit")
        }

        return mapOf(
            "message" to "File uploaded successfully",
            "filename" to file.originalFilename,
            "size" to file.size
            )
     
        
    }
}