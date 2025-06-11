package backend_lingua.linguas.sqs.listener;

import backend_lingua.linguas.kanji.service.FlaskService;
import backend_lingua.linguas.kanji.service.KanjiService;
import backend_lingua.linguas.util.exception.BusinessException;
import backend_lingua.linguas.util.http.HttpResponse;
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

    private final KanjiService kanjiService;
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

                // 🎯 핵심: Flask에서 데이터 가져오기
                processCompletedTask(bookName);
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

    private void processCompletedTask(String bookName) {
        kanjiService.processKanjiData(bookName, flaskDataService.fetchAll().get("details"));
    }
}