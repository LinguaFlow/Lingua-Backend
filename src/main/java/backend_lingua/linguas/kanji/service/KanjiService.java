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
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class KanjiService {

    private final KanjiRepository repository;
    private final S3Service s3Bucket;

    @Transactional
    public Long createKanjiTask(MultipartFile file) {

        String s3Key = uploadFileToS3(file);

        return saveKanjiTask(file.getOriginalFilename(), s3Key);
    }

    public String uploadFileToS3(MultipartFile file) {
        return s3Bucket.upload(file);
    }

    @Transactional
    public Long saveKanjiTask(String fileName, String s3Key) {
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
        String normalizedKey = bookName;

        if (normalizedKey.startsWith("./s3PDF/")) {
            normalizedKey = normalizedKey.substring(2); // "./" 제거
        }

        if (normalizedKey.contains("s3PDF/")) {
            normalizedKey = normalizedKey.substring(normalizedKey.indexOf("s3PDF/") + 6);
        }

        var kanji = findKanjiByS3Key(normalizedKey);

        Map<String, Object> bookData = singletonMap("details", kanjiDetails);

        kanji.completeProcessing(bookData);

        repository.save(kanji);
    }

    @Transactional
    public Kanji findKanjiByS3Key(String s3Key) {
        return repository.findByS3Key(s3Key)
                .orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.KANJI_TASK_NOT_FOUND));
    }

    @Transactional
    public Kanji get(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.KANJI_DATA_PROCESSING_ERROR));
    }
}