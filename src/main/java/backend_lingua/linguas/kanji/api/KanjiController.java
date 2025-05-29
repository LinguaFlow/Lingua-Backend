package backend_lingua.linguas.kanji.api;

import backend_lingua.linguas.kanji.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.kanji.entity.Kanji;
import backend_lingua.linguas.kanji.entity.TaskStatus;

import backend_lingua.linguas.example.service.ExampleService;
import backend_lingua.linguas.kanji.service.KanjiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class KanjiController {

    private final KanjiService kanjiService;
    private final ExampleService exampleService;

    /**
     * 파일 업로드 API
     * 파일을 S3에 업로드하고 작업 ID를 반환합니다.
     * Python 서버가 S3 이벤트를 통해 자동으로 처리합니다.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam MultipartFile file) {
        try {
            // 파일 S3 업로드 + Kanji 엔티티(PENDING) 저장
            Long id = kanjiService.createKanjiTask(file);

            // 202 Accepted와 id 반환
            return ResponseEntity.accepted()
                    .body(Map.of(
                            "id", id.toString(),
                            "message", "파일이 업로드되었습니다. 처리가 완료되면 결과를 확인할 수 있습니다."
                    ));
        } catch (IllegalArgumentException e) {
            // 클라이언트 오류 (잘못된 요청)
            log.warn("Invalid upload request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // 서버 오류
            log.error("Upload processing error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload processing failed");
        }
    }

    /**
     * 작업 상태 조회 API
     * 작업 ID로 현재 상태를 조회합니다.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> status(@PathVariable Long id) {
        try {
            Kanji kanji = kanjiService.get(id);
            TaskStatus status = kanji.getStatus();

            // 상태에 따른 추가 메시지
            String message = switch (status) {
                case PENDING -> "파일이 업로드되어 처리 대기 중입니다.";
                case PROCESSING -> "파일을 처리 중입니다.";
                case DONE -> "처리가 완료되었습니다. /result 엔드포인트에서 결과를 확인할 수 있습니다.";
                case FAILED -> "처리 중 오류가 발생했습니다: " + kanji.getErrorMessage();
            };

            return ResponseEntity.ok(Map.of(
                    "status", status.getStatus(),
                    "code", status.getCode(),
                    "message", message
            ));
        } catch (IllegalArgumentException e) {
            // 찾을 수 없음
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Status check error for id: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Status check failed");
        }
    }

    /**
     * 작업 결과 조회 API
     * 작업이 완료된 경우 결과 데이터를 반환합니다.
     */
    @GetMapping("/{id}/result")
    public ResponseEntity<KanjiVocabularyListResponse> result(@PathVariable Long id) {
        try {
            Kanji kanji = kanjiService.get(id);

            // 상태에 따른 적절한 응답
            return switch (kanji.getStatus()) {
                case PENDING -> ResponseEntity.status(HttpStatus.ACCEPTED)
                        .header("X-Status", "PENDING")
                        .header("X-Message", "파일이 업로드되어 처리 대기 중입니다.")
                        .build();
                case PROCESSING -> ResponseEntity.status(HttpStatus.ACCEPTED)
                        .header("X-Status", "PROCESSING")
                        .header("X-Message", "파일을 처리 중입니다.")
                        .build();
                case DONE ->
                    // 완료된 경우 결과 반환
                        ResponseEntity.ok()
                                .header("X-Status", "DONE")
                                .body(KanjiVocabularyListResponse.from(kanji));
                case FAILED ->
                    // 실패한 경우 오류 메시지 반환
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .header("X-Status", "FAILED")
                                .header("X-Error", kanji.getErrorMessage())
                                .build();
            };
        } catch (IllegalArgumentException e) {
            // 찾을 수 없음
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Result fetch error for id: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Result fetch failed");
        }
    }


    /**
     * 테스트용 API
     * 새 아키텍처에 맞게 메시지 추가
     */
    @PostMapping("/test-upload")
    public ResponseEntity<KanjiVocabularyListResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Kanji kanji = kanjiService.getKanjis();
            return ResponseEntity.ok()
                    .header("X-Test", "true")
                    .body(KanjiVocabularyListResponse.from(kanji));
        } catch (Exception e) {
            log.error("Test upload error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Test upload failed");
        }
    }
}