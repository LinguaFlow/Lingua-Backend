package backend_lingua.linguas.sqs.listener;

import backend_lingua.linguas.kanji.service.FlaskDataService;
import backend_lingua.linguas.kanji.service.KanjiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsCompletionListener {

    private final KanjiService kanjiService;
    private final FlaskDataService flaskDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SqsListener("#{jsonEventQueueName}")
    public void handleCompletionSignal(String messageBody) {
        log.info("📥 SQS 완료 신호 수신: {}", messageBody);

        try {
            // JSON 파싱
            JsonNode signal = objectMapper.readTree(messageBody);

            String status = signal.path("status").asText();
            String bookName = signal.path("book_name").asText();

            log.info("🔍 파싱된 값 - status: '{}', bookName: '{}'", status, bookName);

            // ✅ 수정: "complete"로 변경 (SQS 메시지의 실제 값과 일치)
            if ("complete".equals(status) && !bookName.isEmpty()) {
                log.info("✅ 작업 완료 확인 - 책: {}", bookName);

                // 🎯 핵심: Flask에서 데이터 가져오기
                processCompletedTask(bookName);
            } else {
                log.warn("⚠️ 조건 불일치 - status: '{}' (expected: 'complete'), bookName isEmpty: {}",
                        status, bookName.isEmpty());
            }

        } catch (Exception e) {
            log.error("❌ SQS 완료 신호 처리 실패", e);
        }
    }

    private void processCompletedTask(String bookName) {
        log.info("🚀 Flask 데이터 요청 시작 - 책: {}", bookName);

        try {
            Map<String, Object> flaskData = flaskDataService.fetchAllKanjiDataFromFlask();

            if (flaskData != null) {
                log.info("✅ Flask 데이터 수신 완료, DB 업데이트 시작");

                // 5단계: DB 업데이트
                kanjiService.processKanjiData(bookName, flaskData.get("details"));

                log.info("✅ 전체 처리 완료 - 책: {}", bookName);
            } else {
                log.error("❌ Flask 데이터 가져오기 실패 - 책: {}", bookName);
            }
        } catch (Exception e) {
            log.error("❌ 완료된 작업 처리 중 오류 발생 - 책: {}", bookName, e);
        }
    }
}