package com.recall.backend.service

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PDFExtractionServiceTest {

    @TempDir lateinit var tempDir: Path

    private lateinit var service: PDFExtractionService

    @BeforeEach
    fun setUp() {
        service = PDFExtractionService(tempDir.toString())
    }

    @Test
    fun `should extract text from PDF file`() {
        val documentId = 123L
        copyTestPdfToTempDir(documentId)

        val pages = service.extractText(documentId)

        assertTrue(pages.isNotEmpty())
        pages.forEach { page ->
            assertTrue(page.pageNumber > 0)
            assertNotNull(page.text)
        }
    }

    @Test
    fun `should extract pages with correct page numbers`() {
        val documentId = 456L
        copyTestPdfToTempDir(documentId)

        val pages = service.extractText(documentId)

        pages.forEachIndexed { index, page -> assertEquals(index + 1, page.pageNumber) }
    }

    @Test
    fun `should trim whitespace from page text`() {
        val documentId = 789L
        copyTestPdfToTempDir(documentId)

        val pages = service.extractText(documentId)

        pages.forEach { page -> assertFalse(page.text.startsWith(" ") || page.text.endsWith(" ")) }
    }

    @Test
    fun `should throw exception when file does not exist`() {
        val nonExistentId = 999L

        val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    service.extractText(nonExistentId)
                }

        assertTrue(exception.message?.contains("PDF file not found") == true)
        assertTrue(exception.message?.contains("999") == true)
    }

    private fun copyTestPdfToTempDir(documentId: Long) {
        val testPdfStream = javaClass.getResourceAsStream("/sample-local-pdf.pdf")
        assertNotNull(testPdfStream, "Test PDF not found in resources")

        val targetFile = tempDir.resolve(documentId.toString())
        testPdfStream.use { input ->
            Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
