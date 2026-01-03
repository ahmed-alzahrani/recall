package com.recall.backend.prompts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PromptsTest {

    @Test
    fun `documentSummary should format prompt correctly`() {
        val content = "This is test document content."
        val prompt = Prompts.documentSummary(content)

        val expected =
                """You are helping users understand what a document is about.

Based on these excerpts from a document, write a brief, accessible summary that:
- Explains what the document is about in plain language
- Highlights the main topic or purpose
- Uses simple, everyday words (avoid jargon unless necessary)
- Is 2-3 sentences, maximum 500 characters

Document excerpts:
This is test document content.

Write a helpful summary for someone who hasn't read the document yet:"""

        assertEquals(expected, prompt)
    }

    @Test
    fun `answerQuestion should format prompt with single chunk`() {
        val question = "What is this about?"
        val chunks = listOf("Chunk text here")
        val prompt = Prompts.answerQuestion(question, chunks)

        val expected =
                """You are a helpful assistant answering questions about a document.

Answer the question using the document excerpts below as your PRIMARY source.
When referencing document content, be specific (e.g., "The document explains...", "According to the text...").

You may use general knowledge to:
- Clarify technical terms or concepts
- Provide helpful context or background
- Explain connections between ideas

If the document contradicts common knowledge, trust the document.
If the question cannot be answered from the document, say so clearly, then offer relevant general information if helpful.

Be concise, accurate, and conversational.

Document excerpts:
Chunk text here

Question: What is this about?

Answer:"""

        assertEquals(expected, prompt)
    }

    @Test
    fun `answerQuestion should join multiple chunks with separator`() {
        val chunks = listOf("First chunk", "Second chunk", "Third chunk")
        val prompt = Prompts.answerQuestion("Test question", chunks)

        val expected =
                """            You are a helpful assistant answering questions about a document.
            
            Answer the question using the document excerpts below as your PRIMARY source.
            When referencing document content, be specific (e.g., "The document explains...", "According to the text...").
            
            You may use general knowledge to:
            - Clarify technical terms or concepts
            - Provide helpful context or background
            - Explain connections between ideas
            
            If the document contradicts common knowledge, trust the document.
            If the question cannot be answered from the document, say so clearly, then offer relevant general information if helpful.
            
            Be concise, accurate, and conversational.
            
            Document excerpts:
            First chunk

---

Second chunk

---

Third chunk
            
            Question: Test question
            
            Answer:"""

        assertEquals(expected, prompt)
    }

    @Test
    fun `answerQuestion should handle empty chunks`() {
        val prompt = Prompts.answerQuestion("Test question", emptyList())

        val expected =
                """You are a helpful assistant answering questions about a document.

Answer the question using the document excerpts below as your PRIMARY source.
When referencing document content, be specific (e.g., "The document explains...", "According to the text...").

You may use general knowledge to:
- Clarify technical terms or concepts
- Provide helpful context or background
- Explain connections between ideas

If the document contradicts common knowledge, trust the document.
If the question cannot be answered from the document, say so clearly, then offer relevant general information if helpful.

Be concise, accurate, and conversational.

Document excerpts:


Question: Test question

Answer:"""

        assertEquals(expected, prompt)
    }
}
