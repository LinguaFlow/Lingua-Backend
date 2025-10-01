package backend_lingua.linguas.infrastructure.sqs.listener;

import backend_lingua.linguas.global.exception.SqsErrorHandler;
import backend_lingua.linguas.infrastructure.sqs.service.FlaskService;
import backend_lingua.linguas.domain.vocabulary.service.VocabularyWordServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsCompletionListener {

    private final VocabularyWordServiceImpl vocabularyWordServiceImpl;
    private final SqsErrorHandler errorHandler;
    private final FlaskService flaskDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SqsListener(
            value = "#{jsonEventQueueName}",
            acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL
    )
    public void handleCompletionSignal(String messageBody, Acknowledgement ack) {
        try {
            JsonNode signal = objectMapper.readTree(messageBody);
            String status = signal.path("status").asText();
            String bookName = signal.path("bookName").asText();

            log.info("signal = {}" , signal);
            log.info("status = {}" , status);
            log.info("bookName = {}" , bookName);

            if ("complete".equals(status) && !bookName.isEmpty()) {
                String normalizedS3Key = normalizeToS3Key(bookName);

                log.info("🔑 S3 키 정규화: '{}' → '{}'", bookName, normalizedS3Key);

                processCompletedTask(normalizedS3Key, bookName);
            }
        } catch (JsonProcessingException e) {
            errorHandler.handleSqsError(e, ack);
        }

        ack.acknowledge();
        log.info("✅ 메시지 ACK 완료 - 큐에서 제거됨");
    }

    private void processCompletedTask(String s3Key, String bookName) {
        Map<String, Object> flaskData = flaskDataService.fetchByBookName(bookName);
        Object details = flaskData.get("details");

        if (details == null) {
            log.warn("⚠️ details 필드가 없음, 전체 데이터 사용");
            details = flaskData;
        }

        vocabularyWordServiceImpl.processKanjiData(s3Key, details);
        log.info("✅ 처리 완료");
    }

    private String normalizeToS3Key(String bookName) {
        String s3Key = bookName;
        if (s3Key.startsWith("s3PDF/")) {
            s3Key = s3Key.substring(6);
        }
        if (s3Key.startsWith("./s3PDF/")) {
            s3Key = s3Key.substring(8);
        }
        if (s3Key.contains("/")) {
            s3Key = s3Key.substring(s3Key.lastIndexOf("/") + 1);
        }
        return s3Key;
    }
}