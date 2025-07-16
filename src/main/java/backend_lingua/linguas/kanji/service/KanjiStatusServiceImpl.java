package backend_lingua.linguas.kanji.service;

import backend_lingua.linguas.kanji.dto.response.KanjiStatusResponse;
import backend_lingua.linguas.kanji.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.kanji.entity.Kanji;
import backend_lingua.linguas.kanji.entity.TaskStatus;
import backend_lingua.linguas.kanji.repository.KanjiRepository;
import backend_lingua.linguas.util.exception.BusinessException;
import backend_lingua.linguas.util.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class KanjiStatusServiceImpl implements KanjiStatusService {

    private final KanjiRepository repository;

    @Transactional(readOnly = true)
    @Override
    public KanjiStatusResponse getTaskStatus(Long taskId) {
        Kanji kanji = getKanjiById(taskId);
        TaskStatus status = kanji.getStatus();
        String message = status.getMessage(kanji);

        return KanjiStatusResponse.of(
                status.getStatus(),
                status.getCode(),
                message
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<KanjiVocabularyListResponse> getTaskResult(Long taskId) {
        Kanji kanji = getKanjiById(taskId);
        return kanji.getStatus().buildResultResponse(kanji);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean canProcessTask(Long taskId) {
        Kanji kanji = getKanjiById(taskId);
        return kanji.getStatus().canBeProcessed();
    }

    private Kanji getKanjiById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));
    }
}
