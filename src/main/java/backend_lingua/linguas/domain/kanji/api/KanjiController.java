package backend_lingua.linguas.domain.kanji.api;

import backend_lingua.linguas.domain.kanji.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.domain.kanji.entity.Kanji;
import backend_lingua.linguas.domain.kanji.entity.TaskStatus;
import backend_lingua.linguas.domain.kanji.service.FlaskService;
import backend_lingua.linguas.domain.kanji.service.KanjiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class KanjiController {

    private final KanjiService kanjiService;
    private final FlaskService dataService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam MultipartFile file) {
        Long id = kanjiService.createKanjiTask(file);

        return ResponseEntity.accepted()
                .body(Map.of(
                        "id", id.toString(),
                        "message", "파일이 업로드되었습니다. 처리가 완료되면 결과를 확인할 수 있습니다."
                ));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> status(@PathVariable Long id) {
        Kanji kanji = kanjiService.get(id);

        TaskStatus status = kanji.getStatus();

        String message = status.getMessage(kanji);

        return ResponseEntity.ok(Map.of(
                "status", status.getStatus(),
                "code", status.getCode(),
                "message", message
        ));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<KanjiVocabularyListResponse> result(@PathVariable Long id) {
        Kanji kanji = kanjiService.get(id);

        return kanji.getStatus().buildResultResponse(kanji);
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> stringObjectMap = dataService.fetchAll();
        return ResponseEntity.ok().body(stringObjectMap);
    }
}