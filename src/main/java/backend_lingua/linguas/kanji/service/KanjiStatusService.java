package backend_lingua.linguas.kanji.service;

import backend_lingua.linguas.kanji.dto.response.KanjiStatusResponse;
import backend_lingua.linguas.kanji.dto.response.KanjiVocabularyListResponse;
import org.springframework.http.ResponseEntity;

public interface KanjiStatusService {

    KanjiStatusResponse getTaskStatus(Long taskId);

    ResponseEntity<KanjiVocabularyListResponse> getTaskResult(Long taskId);

    boolean canProcessTask(Long taskId);
}
