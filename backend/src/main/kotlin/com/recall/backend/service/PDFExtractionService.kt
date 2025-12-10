package com.recall.backend.service

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.io.path.Path

@Service
class PDFExtractionService(
    @Value("\${app.upload.tmp-dir}") private val tmpFileStoragePath: String,
) {
    data class PageText(
        val pageNumber: Int,
        val text: String,
    )

    private val tmpFileStorage = Path(tmpFileStoragePath)

    fun extractText(documentId: Long): List<PageText> {
        val filePath = tmpFileStorage.resolve(documentId.toString())
        
        return Loader.loadPDF(filePath.toFile()).use { pdfDocument ->
            val textStripper = PDFTextStripper()
            val pages = mutableListOf<PageText>()
            
            for (pageNum in 1..pdfDocument.numberOfPages) {
                textStripper.startPage = pageNum
                textStripper.endPage = pageNum
                val pageText = textStripper.getText(pdfDocument)
                
                pages.add(PageText(
                    pageNumber = pageNum,
                    text = pageText.trim()
                ))
            }
            
            pages
        }
    }
}