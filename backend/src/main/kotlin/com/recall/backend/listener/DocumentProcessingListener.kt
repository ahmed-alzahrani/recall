import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import com.recall.backend.repository.DocumentRepository
import com.recall.backend.model.DocumentStatus

@Component
class DocumentProcessingListener(
    private val documentRepository: DocumentRepository,
) {

    @RabbitListener(queues = ["document.processing"])
    fun processDocument(documentId: Long) {
        val document = documentRepository.findById(documentId)
    }
}
