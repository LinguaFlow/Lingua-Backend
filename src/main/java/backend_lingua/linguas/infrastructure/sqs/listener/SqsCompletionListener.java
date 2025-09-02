package backend_lingua.linguas.infrastructure.sqs.listener;

import backend_lingua.linguas.domain.vocabulary.service.FlaskService;
import backend_lingua.linguas.domain.vocabulary.service.VocabularyWordServiceImpl;
import backend_lingua.linguas.global.exception.BusinessException;
import backend_lingua.linguas.global.dto.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsCompletionListener {

    private final VocabularyWordServiceImpl vocabularyWordServiceImpl;
    private final FlaskService flaskDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SqsListener("#{jsonEventQueueName}")
    public void handleCompletionSignal(String messageBody, Acknowledgement ack) {
        log.info("📥 SQS 완료 신호 수신: {}", messageBody);

        try {
            JsonNode signal = objectMapper.readTree(messageBody);

            String status = signal.path("status").asText();
            String bookName = signal.path("book_name").asText();

            log.info("🔍 파싱된 값 - status: '{}', bookName: '{}'", status, bookName);

            if ("complete".equals(status) && !bookName.isEmpty()) {
                log.info("✅ 작업 완료 확인 - 책: {}", bookName);

                String normalizedS3Key = normalizeToS3Key(bookName);
                log.info("🔑 S3 키 정규화: '{}' → '{}'", bookName, normalizedS3Key);

                processCompletedTask(normalizedS3Key);
            } else {
                log.warn("⚠️ 조건 불일치 - status: '{}' (expected: 'complete'), bookName isEmpty: {}", status, bookName.isEmpty());
            }
        } catch (JsonProcessingException e) {
            log.error("❌ SQS 메시지 JSON 파싱 실패 - 메시지 삭제", e);
            ack.acknowledge(); // 메시지 삭제하여 재시도 방지
            throw new BusinessException(HttpResponse.FailureStatus.SQS_MESSAGE_PARSE_ERROR);
        }
    }

    private void processCompletedTask(String s3Key) {
        vocabularyWordServiceImpl.processKanjiData(s3Key, flaskDataService.fetchAll().get("details"));
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