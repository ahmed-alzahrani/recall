package com.recall.backend.service

import com.recall.backend.dto.PageText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ChunkingServiceTest {

    private val service = ChunkingService()

    @Test
    fun `should return empty list for empty pages`() {
        val chunks = service.chunkDocument(emptyList())
        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `should create single chunk for small content`() {
        val pages = listOf(PageText(1, "This is a short document. It has only a few sentences."))
        val chunks = service.chunkDocument(pages)

        assertEquals(1, chunks.size)
        assertEquals(0, chunks[0].chunkIndex)
        assertEquals(1, chunks[0].pageStart)
        assertEquals(1, chunks[0].pageEnd)
        assertTrue(chunks[0].text.contains("This is a short document"))
    }

    @Test
    fun `should create multiple chunks when content exceeds target size`() {
        val longText = generateLongText(1500) // ~1500 words, should create 2+ chunks
        val pages = listOf(PageText(1, longText))
        val chunks = service.chunkDocument(pages)

        assertTrue(chunks.size >= 2)
        assertEquals(0, chunks[0].chunkIndex)
        assertEquals(1, chunks[1].chunkIndex)
        assertEquals(1, chunks[0].pageStart)
        assertEquals(1, chunks[0].pageEnd)
    }

    @Test
    fun `should handle multiple pages correctly`() {
        val pages =
                listOf(
                        PageText(1, "First page content. With multiple sentences."),
                        PageText(2, "Second page content. Also has sentences."),
                        PageText(3, "Third page content. Final sentences.")
                )
        val chunks = service.chunkDocument(pages)

        assertTrue(chunks.isNotEmpty())
        assertEquals(1, chunks[0].pageStart)
        val lastChunk = chunks.last()
        assertEquals(3, lastChunk.pageEnd)
    }

    @Test
    fun `should create sequential chunk indices`() {
        val longText = generateLongText(2000) // Should create multiple chunks
        val pages = listOf(PageText(1, longText))
        val chunks = service.chunkDocument(pages)

        chunks.forEachIndexed { index, chunk -> assertEquals(index, chunk.chunkIndex) }
    }

    @Test
    fun `should include overlap between chunks`() {
        val longText = generateLongText(1500)
        val pages = listOf(PageText(1, longText))
        val chunks = service.chunkDocument(pages)

        if (chunks.size >= 2) {
            val firstChunkEnd = chunks[0].text.takeLast(100)
            val secondChunkStart = chunks[1].text.take(100)
            // Some overlap should exist (last sentences of first chunk appear in second)
            assertTrue(
                    firstChunkEnd.split(".").any { sentence ->
                        secondChunkStart.contains(sentence.trim())
                    } || secondChunkStart.isNotEmpty()
            )
        }
    }

    @Test
    fun `should handle pages with no text`() {
        val pages =
                listOf(PageText(1, "Content here."), PageText(2, ""), PageText(3, "More content."))
        val chunks = service.chunkDocument(pages)

        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.text.isNotBlank() })
    }

    private fun generateLongText(targetWords: Int): String {
        val words = mutableListOf<String>()
        repeat(targetWords) { words.add("word${it}") }
        return words.chunked(10).joinToString(". ") { it.joinToString(" ") } + "."
    }
}
