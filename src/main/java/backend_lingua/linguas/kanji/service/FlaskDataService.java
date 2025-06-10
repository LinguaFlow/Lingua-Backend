package backend_lingua.linguas.kanji.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class FlaskDataService {

    private final RestTemplate restTemplate;

    @Value("${kanji.api.base-url}")  // 또는 ${kanji.api.url}
    private String flaskBaseUrl;

    /**
     * 3단계: Flask 서버에서 전체 한자 데이터를 GET 요청으로 가져오기
     */
    public Map<String, Object> fetchAllKanjiDataFromFlask() {
        try {
            String flaskUrl = flaskBaseUrl + "/api/kanji/all";

            log.info("📡 Flask에 GET 요청: {}", flaskUrl);

            // 🎯 핵심: Flask에 GET 요청 전송
            ResponseEntity<Map> response = restTemplate.getForEntity(flaskUrl, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = response.getBody();

                log.info("✅ Flask 데이터 수신 성공 - 항목수: {}", getDetailCount(data));
                return data;
            }

            log.warn("❌ Flask 응답 오류: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("❌ Flask 연결 실패", e);
            return null;
        }
    }

    private int getDetailCount(Map<String, Object> data) {
        Object details = data.get("details");
        if (details instanceof java.util.List) {
            return ((java.util.List<?>) details).size();
        }
        return 0;
    }
}