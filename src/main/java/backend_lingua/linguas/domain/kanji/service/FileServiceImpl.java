package backend_lingua.linguas.domain.kanji.service;

import backend_lingua.linguas.domain.kanji.dto.response.FileUploadResponse;
import backend_lingua.linguas.domain.kanji.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.domain.kanji.entity.File;
import backend_lingua.linguas.domain.kanji.enumerated.TaskStatus;
import backend_lingua.linguas.domain.kanji.repository.FileRepository;
import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.infrastructure.s3.service.S3Service;
import backend_lingua.linguas.global.exception.BusinessException;
import backend_lingua.linguas.global.dto.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl {

    private final FileRepository repository;
    private final S3Service s3Bucket;

    @Transactional
    public FileUploadResponse uploadFile(Member member, MultipartFile file) {

        String s3Key = s3Bucket.upload(file);

        return saveKanjiTask(member, file.getOriginalFilename(), s3Key);
    }

    private FileUploadResponse saveKanjiTask(Member member, String fileName, String s3Key) {
        File file = File.builder()
                .bookName(Optional.ofNullable(fileName).orElse("unnamed"))
                .s3Key(s3Key)
                .status(TaskStatus.PENDING)
                .member(member)
                .build();

        File result = repository.save(file);

        return FileUploadResponse.builder()
                .taskId(result.getId())
                .fileName(result.getBookName())
                .status(result.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public FileUploadResponse uploadStatus(Long fileId) {

        File file = get(fileId);

        return FileUploadResponse.builder()
                .taskId(file.getId())
                .fileName(file.getBookName())
                .status(file.getStatus())
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

//    public KanjiVocabularyListResponse getCompletedTask(Long fileId) {
//
//        File file = repository.findById(fileId).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));
//
////        return file.getStatus().buildResultResponse(file);
//    }



    @Transactional(readOnly = true)
    public File get(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));
    }

}