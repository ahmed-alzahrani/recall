package com.recall.backend.service

import com.recall.backend.dto.PageText
import kotlin.io.path.Path
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PDFExtractionService(
        @Value("\${app.upload.tmp-dir}") private val tmpFileStoragePath: String,
) {

    private val tmpFileStorage = Path(tmpFileStoragePath)

    fun extractText(documentId: Long): List<PageText> {
        val filePath = tmpFileStorage.resolve(documentId.toString())
        val file = filePath.toFile()

        if (!file.exists()) {
            throw IllegalArgumentException("PDF file not found for document ID: $documentId")
        }

        return try {
            Loader.loadPDF(file).use { pdfDocument -> extractAllPages(pdfDocument) }
        } catch (e: Exception) {
            throw RuntimeException(
                    "Failed to extract text from PDF for document ID: $documentId",
                    e
            )
        }
    }

    private fun extractAllPages(pdfDocument: PDDocument): List<PageText> {
        val textStripper = PDFTextStripper()
        val pages = mutableListOf<PageText>()

        for (pageNum in 1..pdfDocument.numberOfPages) {
            pages.add(extractPage(pdfDocument, textStripper, pageNum))
        }

        return pages
    }

    private fun extractPage(
            pdfDocument: PDDocument,
            textStripper: PDFTextStripper,
            pageNum: Int
    ): PageText {
        textStripper.startPage = pageNum
        textStripper.endPage = pageNum
        val pageText = textStripper.getText(pdfDocument)

        return PageText(pageNumber = pageNum, text = pageText.trim())
    }
}
