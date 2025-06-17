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
    public void processKanjiData(String bookName, Object kanjiDetails) {
        String s3Key = bookName;

        if (s3Key.startsWith("s3PDF/")) {
            s3Key = s3Key.substring(6); // "s3PDF/" 제거
        }

        if (s3Key.startsWith("./s3PDF/")) {
            s3Key = s3Key.substring(8); // "./s3PDF/" 제거
        }

        if (s3Key.contains("/")) {
            s3Key = s3Key.substring(s3Key.lastIndexOf("/") + 1);
        }

        var kanji = repository.findByS3Key(s3Key).orElseThrow(()
                -> new BusinessException(HttpResponse.FailureStatus.KANJI_TASK_NOT_FOUND));

        Map<String, Object> bookData = Collections.singletonMap("details", kanjiDetails);

        kanji.completeProcessing(bookData);

        repository.save(kanji);
    }

    @Transactional
    public Kanji get(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));
    }

}