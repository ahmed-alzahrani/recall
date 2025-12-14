import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import com.recall.backend.service.PDFExtractionService
import com.recall.backend.service.ChunkingService
import com.recall.backend.service.EmbeddingService
import com.recall.backend.model.DocumentStatus

@Component
class DocumentProcessingListener(
    private val pdfExtractionService: PDFExtractionService,
    private val chunkingService: ChunkingService,
    private val embeddingService: EmbeddingService,
) {

    @RabbitListener(queues = ["document.processing"])
    fun processDocument(documentId: Long) {
        val pages = pdfExtractionService.extractText(documentId)
        val chunks = chunkingService.chunkDocument(pages)
        val chunksWithEmbeddings = embeddingService.embedChunks(chunks)

        
    }
}
