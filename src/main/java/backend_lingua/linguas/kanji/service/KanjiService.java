package backend_lingua.linguas.kanji.service;

import backend_lingua.linguas.kanji.entity.Kanji;
import backend_lingua.linguas.kanji.entity.TaskStatus;
import backend_lingua.linguas.kanji.repository.KanjiRepository;
import backend_lingua.linguas.s3.service.S3Service;
import backend_lingua.linguas.util.exception.BusinessException;
import backend_lingua.linguas.util.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KanjiService {

    private final KanjiRepository repository;
    private final S3Service s3Bucket;

    @Transactional
    public Long createKanjiTask(MultipartFile file) {

        String s3Key = s3Bucket.upload(file);

        return saveKanjiTask(file.getOriginalFilename(), s3Key);
    }

    private Long saveKanjiTask(String fileName, String s3Key) {
        Kanji kanji = Kanji.builder()
                .bookName(Optional.ofNullable(fileName).orElse("unnamed"))
                .s3Key(s3Key)
                .status(TaskStatus.PENDING)
                .build();

        repository.save(kanji);

        return kanji.getId();
    }

    @Transactional
    public void processKanjiData(String s3Key, Object kanjiDetails) {

        log.info("s3eky={}" , s3Key);
        var kanji = repository.findByS3Key(s3Key).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.KANJI_TASK_NOT_FOUND));

        if (!kanji.getStatus().canBeProcessed()) {
            log.warn("⚠️ 처리 불가능한 상태 - 현재 상태: {}, Task ID: {}, S3 Key: {}",
                    kanji.getStatus().getStatus(), kanji.getId(), s3Key);
            throw new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST);
        }

        try {
            Map<String, Object> bookData = Collections.singletonMap("details", kanjiDetails);

            kanji.completeProcessing(bookData);

            log.info("✅ Kanji 작업 처리 완료 - Task ID: {}", kanji.getId());
        } catch (Exception e) {
            log.error("❌ Kanji 작업 처리 실패 - ID: {}, 파일명: '{}', 오류: {}", kanji.getId(), kanji.getBookName(), e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Kanji get(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));
    }

}