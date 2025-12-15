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
}
