package com.recall.backend.prompts

object Prompts {
    fun documentSummary(content: String): String {
        return """
        You are helping users understand what a document is about.
        
        Based on these excerpts from a document, write a brief, accessible summary that:
        - Explains what the document is about in plain language
        - Highlights the main topic or purpose
        - Uses simple, everyday words (avoid jargon unless necessary)
        - Is 2-3 sentences, maximum 500 characters
        
        Document excerpts:
        $content
        
        Write a helpful summary for someone who hasn't read the document yet:
    """.trimIndent()
    }

    fun answerQuestion(question: String, chunkTexts: List<String>): String {
        val context = chunkTexts.joinToString("\n\n---\n\n")
        return """
            You are a helpful assistant answering questions about a document.
            
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
            $context
            
            Question: $question
            
            Answer:
        """.trimIndent()
    }
}
