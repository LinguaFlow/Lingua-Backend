package backend_lingua.linguas.global.exception

import backend_lingua.linguas.global.dto.HttpResponse
import com.fasterxml.jackson.core.JsonProcessingException
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import lombok.extern.slf4j.Slf4j
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
@Slf4j
class SqsErrorHandler {

    fun handleSqsError(
        exception: Exception,
        acknowledgement: Acknowledgement?
    ) {
        val errorDetails = when (exception) {
            is JsonProcessingException -> {
                BusinessException(HttpResponse.FailureStatus.SQS_MESSAGE_PARSE_ERROR)
            }
            is BusinessException -> {
                exception
            }
            else -> {
                BusinessException(HttpResponse.FailureStatus.SQS_KANJI_DATA_PROCESSING_ERROR)
            }
        }

        // DLQ(Dead Letter Queue)로 이동하거나 재시도 로직
        handleFailedMessage(acknowledgement, errorDetails)
    }

    private fun handleFailedMessage(
        ack: Acknowledgement?,
        error: BusinessException
    ) {
        // 재시도 횟수 확인 후 DLQ로 이동 또는 ACK 처리
        ack?.acknowledge()
    }
}