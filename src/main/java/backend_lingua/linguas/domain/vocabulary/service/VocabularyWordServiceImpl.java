package backend_lingua.linguas.domain.vocabulary.service;

import backend_lingua.linguas.domain.vocabulary.dto.KanjiVocabularyListResponse;
import backend_lingua.linguas.domain.vocabulary.dto.UploadTaskStatusResponse;
import backend_lingua.linguas.domain.vocabulary.entity.VocabularyWord;
import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;
import backend_lingua.linguas.domain.vocabulary.event.UploadStatusEventPublisher;
import backend_lingua.linguas.domain.vocabulary.repository.VocabularyWordJdbcRepository;
import backend_lingua.linguas.domain.vocabulary.repository.VocabularyWordRepository;
import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.infrastructure.s3.service.S3Service;
import backend_lingua.linguas.global.exception.BusinessException;
import backend_lingua.linguas.global.dto.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularyWordServiceImpl implements VocabularyWordService {

    private final VocabularyWordRepository vocabularyWordRepository;
    private final VocabularyWordJdbcRepository vocabularyWordJdbcRepository;
    private final UploadStatusEventPublisher eventPublisher;
    private final S3Service s3Bucket;

    @Transactional
    public UploadTaskStatusResponse uploadFile(Member member, MultipartFile file) {
        String s3Key = s3Bucket.upload(file);

        UploadTaskStatusResponse response = saveVocabularyWord(member, file.getOriginalFilename(), s3Key);

        eventPublisher.publishStatusUpdate(response.getTaskId(), TaskStatus.PENDING);

        return response;
    }

    private UploadTaskStatusResponse saveVocabularyWord(Member member, String fileName, String s3Key) {
        VocabularyWord vocabularyWord = VocabularyWord.builder()
                .bookName(Optional.ofNullable(fileName).orElse("unnamed"))
                .s3Key(s3Key)
                .status(TaskStatus.PENDING)
                .member(member)
                .build();

        VocabularyWord result = vocabularyWordRepository.save(vocabularyWord);

        return UploadTaskStatusResponse.builder()
                .taskId(result.getId())
                .fileName(result.getBookName())
                .status(result.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public UploadTaskStatusResponse uploadTaskStatus(Long fileId) {

        return vocabularyWordJdbcRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.USER_NOT_FOUND));
    }

    @Transactional
    public void processKanjiData(String s3Key, Object kanjiDetails) {
        StopWatch stopWatch = new StopWatch("CreateEvent");

        stopWatch.start("getEvent");
        var file = vocabularyWordRepository.findByS3Key(s3Key)
                .orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.KANJI_TASK_NOT_FOUND));
        stopWatch.stop();

        if (!file.getStatus().canBeProcessed()) {
            log.warn("⚠️ 처리 불가능한 상태 - 현재 상태: {}, Task ID: {}, S3 Key: {}",
                    file.getStatus().getStatus(), file.getId(), s3Key);
            throw new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST);
        }

        try {
            stopWatch.start("saveEvent");
            Map<String, Object> bookData = Collections.singletonMap("details", kanjiDetails);

            file.completeProcessing(bookData);

            eventPublisher.publishStatusUpdate(file.getId(), TaskStatus.DONE);


            log.info("✅ Kanji 작업 처리 완료 - Task ID: {}", file.getId());
            stopWatch.stop();
        } catch (Exception e) {
            eventPublisher.publishStatusUpdate(file.getId(), TaskStatus.FAILED);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public KanjiVocabularyListResponse getCompletedTask(Long fileId) {

        VocabularyWord vocabularyWord =  get(fileId);

        if (vocabularyWord.getStatus() != TaskStatus.DONE) {
            throw new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST);
        }

        return KanjiVocabularyListResponse.from(vocabularyWord);
    }

    @Transactional
    public void deleteVocabularyWord(Long id) {
        VocabularyWord vocabularyWord = vocabularyWordRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.USER_NOT_FOUND));

        vocabularyWord.delete();

        vocabularyWordRepository.save(vocabularyWord);
    }

    @Transactional
    public void cancelUpload(Member member, Long fileId) {
        VocabularyWord vocabularyWord = vocabularyWordRepository
                .findById(fileId)
                .orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));

        if (!vocabularyWord.getMember().getId().equals(member.getId())) {
            throw new BusinessException(HttpResponse.FailureStatus.USER_NOT_FOUND);
        }

        if (!vocabularyWord.getStatus().isCancellable()) {
            throw new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST);
        }

        s3Bucket.delete(vocabularyWord.getS3Key());

        vocabularyWordRepository.deleteById(vocabularyWord.getId());

        eventPublisher.publishStatusUpdate(fileId, TaskStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public VocabularyWord get(Long id) {
        return vocabularyWordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.USER_NOT_FOUND));
    }
}