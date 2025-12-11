package com.recall.backend.service

import org.springframework.stereotype.Service
import com.recall.backend.dto.PageText
import com.recall.backend.dto.ChunkData

@Service
class ChunkingService {
    private val targetWordsPerChunk = 680
    private val overlapWords = 67
    private val minChunkWords = 600
    
    fun chunkDocument(pages: List<PageText>): List<ChunkData> {
        val sentences = mutableListOf<SentenceWithPage>()
        for (page in pages) {
            val pageSentences = splitIntoSentences(page.text)
            for (sentence in pageSentences) {
                if (sentence.isNotBlank()) {
                    sentences.add(SentenceWithPage(
                        text = sentence.trim(),
                        pageNumber = page.pageNumber,
                        wordCount = countWords(sentence)
                    ))
                }
            }
        }
        
        if (sentences.isEmpty()) {
            return emptyList()
        }
        
        val chunks = mutableListOf<ChunkData>()
        var currentChunkWords = 0
        var currentChunkSentences = mutableListOf<String>()
        var chunkStartPage = sentences.first().pageNumber
        var chunkIndex = 0
        
        for (i in sentences.indices) {
            val sentence = sentences[i]
            val wouldExceed = currentChunkWords + sentence.wordCount > targetWordsPerChunk
            
            if (wouldExceed && currentChunkWords >= minChunkWords) {
                // Finalize current chunk at sentence boundary
                val chunkText = currentChunkSentences.joinToString(" ")
                chunks.add(ChunkData(
                    text = chunkText,
                    chunkIndex = chunkIndex++,
                    pageStart = chunkStartPage,
                    pageEnd = sentences[i - 1].pageNumber
                ))
                
                // Start new chunk with overlap
                val overlapSentences = getOverlapSentences(
                    currentChunkSentences,
                    overlapWords
                )
                currentChunkSentences = overlapSentences.toMutableList()
                currentChunkWords = overlapSentences.sumOf { countWords(it) }
                chunkStartPage = sentence.pageNumber
            }
            
            currentChunkSentences.add(sentence.text)
            currentChunkWords += sentence.wordCount
        }
        
        if (currentChunkSentences.isNotEmpty()) {
            chunks.add(ChunkData(
                text = currentChunkSentences.joinToString(" "),
                chunkIndex = chunkIndex,
                pageStart = chunkStartPage,
                pageEnd = sentences.last().pageNumber
            ))
        }
        
        return chunks
    }
    
    private fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
    }
    
    private fun countWords(text: String): Int {
        return text.split(Regex("\\s+")).size
    }
    
    private fun getOverlapSentences(
        sentences: List<String>,
        targetWords: Int
    ): List<String> {
        var wordCount = 0
        val overlap = mutableListOf<String>()
        
        for (i in sentences.indices.reversed()) {
            val sentenceWords = countWords(sentences[i])
            if (wordCount + sentenceWords <= targetWords) {
                overlap.add(0, sentences[i])
                wordCount += sentenceWords
            } else {
                break
            }
        }
        
        return overlap
    }
    
    private data class SentenceWithPage(
        val text: String,
        val pageNumber: Int,
        val wordCount: Int
    )
}