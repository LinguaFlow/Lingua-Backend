package backend_lingua.linguas.sqs.listener;

import backend_lingua.linguas.kanji.service.KanjiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.String; // String 클래스 임포트 추가

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsEventListener {
    private final KanjiService kanjiService;

    private final ObjectMapper om = new ObjectMapper();

    @SqsListener("#{jsonEventQueueName}")
    public void handle(String body) {
        try {
            // JSON 파싱
            JsonNode node = om.readTree(body);
            log.info("📤 파싱된 메시지 구조: {}", node.toString());

            // Python에서 처리된 결과 메시지 형식 확인
            if (node.has("book_name") && node.has("details")) {
                // Python 서버에서 처리가 완료된 결과 데이터
                String bookName = node.path("book_name").asText();

                JsonNode details = node.path("details");

                node.path("pages_len").asInt(0);

                // KanjiService를 통해 데이터 처리 및 작업 상태 업데이트
                kanjiService.processKanjiData(bookName, details);

                log.info("✅ 책 데이터 처리 및 작업 상태 업데이트 완료: {}", bookName);
            } else {
                // 알 수 없는 형식의 메시지
                log.warn("⚠️ 알 수 없는 형식의 메시지를 수신했습니다. 처리를 건너뜁니다.");
            }
        } catch (JsonProcessingException e) {
            // JSON 처리 중 발생하는 특정 예외 처리
            log.error("❌ JSON 처리 중 오류 발생", e);
        } catch (Exception e) {
            // 기타 모든 예외 처리
            log.error("❌ SQS 메시지 처리 중 오류 발생", e);
        }
    }
}