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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    private String uploadFileToS3(MultipartFile file) {
        return s3Bucket.upload(file);
    }

    private Long saveKanjiTask(String fileName, String s3Key) {
        Kanji kanji = Kanji.builder()
                .bookName(Optional.ofNullable(fileName).orElse("unnamed"))
                .s3Key(s3Key)
                .status(TaskStatus.PENDING)
                .build();

        repository.save(kanji);

        log.info("작업 생성 완료 - id: {}, 상태: {}, 코드: {}, S3 키: {}", kanji.getId(), TaskStatus.PENDING.getStatus(), TaskStatus.PENDING.getCode(), s3Key);

        return kanji.getId();
    }

    @Transactional
    public void processKanjiData(String bookName, Object kanjiDetails) {
        log.info("Python에서 처리된 책 데이터 수신 - book_name: {}, 항목 수: {}", bookName, (kanjiDetails instanceof Iterable) ? ((Iterable<?>) kanjiDetails).spliterator().getExactSizeIfKnown() : -1);

        String normalizedKey = bookName;

        if (normalizedKey.startsWith("./s3PDF/")) {
            normalizedKey = normalizedKey.substring(2); // "./" 제거
        }

        if (normalizedKey.contains("s3PDF/")) {
            normalizedKey = normalizedKey.substring(normalizedKey.indexOf("s3PDF/") + 6);
        }

        String s3Key = normalizedKey;

        Kanji kanji = repository.findByS3Key(s3Key).orElseThrow(() -> new IllegalArgumentException(""));

        Map<String, Object> bookData = new HashMap<>();
        bookData.put("details", kanjiDetails);

        kanji.completeProcessing(bookData);

        repository.save(kanji);

        log.info("작업 완료 - id: {}, 상태: {}, 코드: {}", kanji.getId(), TaskStatus.DONE.getStatus(), TaskStatus.DONE.getCode());
    }

    public Kanji get(Long id) {
        Kanji kanji = repository.findById(id).orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));

        TaskStatus status = kanji.getStatus();
        log.info("작업 조회 - id: {}, 상태: {}, 코드: {}", id, status.getStatus(), status.getCode());

        return kanji;
    }

    /* Test을 위한 메소드 추후 삭제 예정 */
    public Kanji getKanjis() {
        return repository.findById(22L).orElseThrow(() -> new IllegalArgumentException("Test entry not found"));
    }
}