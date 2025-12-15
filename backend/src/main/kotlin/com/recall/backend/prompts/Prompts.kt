package com.recall.backend.prompts

object Prompts {
    fun documentSummary(content: String): String {
        return """
            Based on the following document excerpts, provide a concise summary in 2-3 sentences (maximum 500 characters):
            
            $content
        """.trimIndent()
    }
}