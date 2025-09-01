package backend_lingua.linguas.domain.vocabulary.service;

import backend_lingua.linguas.domain.vocabulary.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.domain.vocabulary.dto.response.UploadTaskStatusResponse;
import backend_lingua.linguas.domain.vocabulary.entity.VocabularyWord;
import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;
import backend_lingua.linguas.domain.vocabulary.repository.VocabularyWordRepository;
import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.infrastructure.s3.service.S3Service;
import backend_lingua.linguas.global.exception.BusinessException;
import backend_lingua.linguas.global.dto.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularyWordServiceImpl implements VocabularyWordService {

    private final VocabularyWordRepository repository;
    private final S3Service s3Bucket;

    @Transactional
    public UploadTaskStatusResponse uploadFile(Member member, MultipartFile file) {

        String s3Key = s3Bucket.upload(file);

        return saveVocabularyWord(member, file.getOriginalFilename(), s3Key);
    }

    @Transactional(readOnly = true)
    public UploadTaskStatusResponse uploadTaskStatus(Long fileId) {

        VocabularyWord vocabularyWord = get(fileId);

        return UploadTaskStatusResponse.builder()
                .taskId(vocabularyWord.getId())
                .fileName(vocabularyWord.getBookName())
                .status(vocabularyWord.getStatus())
                .build();
    }

    public KanjiVocabularyListResponse getCompletedTask(Long fileId) {

        VocabularyWord vocabularyWord = repository.findById(fileId).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));

        return KanjiVocabularyListResponse.from(vocabularyWord);
    }

    private UploadTaskStatusResponse saveVocabularyWord(Member member, String fileName, String s3Key) {
        VocabularyWord vocabularyWord = VocabularyWord.builder()
                .bookName(Optional.ofNullable(fileName).orElse("unnamed"))
                .s3Key(s3Key)
                .status(TaskStatus.PENDING)
                .member(member)
                .build();

        VocabularyWord result = repository.save(vocabularyWord);

        return UploadTaskStatusResponse.builder()
                .taskId(result.getId())
                .fileName(result.getBookName())
                .status(result.getStatus())
                .build();
    }

    @Transactional
    public void processKanjiData(String s3Key, Object kanjiDetails) {

        log.info("s3eky={}", s3Key);
        var file = repository.findByS3Key(s3Key).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.KANJI_TASK_NOT_FOUND));

        if (!file.getStatus().canBeProcessed()) {
            log.warn("⚠️ 처리 불가능한 상태 - 현재 상태: {}, Task ID: {}, S3 Key: {}",
                    file.getStatus().getStatus(), file.getId(), s3Key);
            throw new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST);
        }

        try {
            Map<String, Object> bookData = Collections.singletonMap("details", kanjiDetails);

            file.completeProcessing(bookData);

            log.info("✅ Kanji 작업 처리 완료 - Task ID: {}", file.getId());
        } catch (Exception e) {
            log.error("❌ Kanji 작업 처리 실패 - ID: {}, 파일명: '{}', 오류: {}", file.getId(), file.getBookName(), e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public VocabularyWord get(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));
    }

}