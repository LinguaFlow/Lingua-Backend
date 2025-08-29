package backend_lingua.linguas.infrastructure.sqs.listener;

import backend_lingua.linguas.domain.kanji.service.FlaskService;
import backend_lingua.linguas.domain.kanji.service.FileServiceImpl;
import backend_lingua.linguas.global.exception.BusinessException;
import backend_lingua.linguas.global.dto.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsCompletionListener {

    private final FileServiceImpl fileServiceImpl;
    private final FlaskService flaskDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SqsListener("#{jsonEventQueueName}")
    public void handleCompletionSignal(String messageBody) {
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
        } catch (BusinessException e) {
            log.error("❌ 비즈니스 로직 처리 실패: {}", e.getMessage());
            throw e; // BusinessException은 그대로 재던지기
        } catch (JsonProcessingException e) {
            log.error("❌ SQS 메시지 JSON 파싱 실패", e);
            throw new BusinessException(HttpResponse.FailureStatus.SQS_MESSAGE_PARSE_ERROR);
        } catch (Exception e) {
            log.error("❌ SQS 완료 신호 처리 중 예상치 못한 오류", e);
            throw new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST);
        }
    }

    private void processCompletedTask(String s3Key) {
        fileServiceImpl.processKanjiData(s3Key, flaskDataService.fetchAll().get("details"));
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