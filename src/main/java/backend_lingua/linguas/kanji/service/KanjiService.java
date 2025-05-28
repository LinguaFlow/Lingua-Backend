package backend_lingua.linguas.kanji.service;

import backend_lingua.linguas.kanji.entity.Kanji;
import backend_lingua.linguas.kanji.entity.TaskStatus;
import backend_lingua.linguas.kanji.repository.KanjiRepository;
import backend_lingua.linguas.s3.service.S3Service;
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

    /* 업로드 단계 */
    @Transactional
    public Long createKanjiTask(MultipartFile file) {
        // S3에 파일 업로드하고 키 얻기
        String s3Key = uploadFileToS3(file);

        // 작업 생성 및 저장
        return saveKanjiTask(file.getOriginalFilename(), s3Key);
    }

    /**
     * 파일을 S3에 업로드합니다.
     *
     * @param file 업로드할 파일
     * @return 업로드된 파일의 S3 키
     */
    private String uploadFileToS3(MultipartFile file) {
        return s3Bucket.upload(file);
    }

    /**
     * 칸지 작업을 저장소에 저장합니다.
     *
     * @param fileName 파일 이름
     * @param s3Key    S3 키
     * @return 저장된 작업의 ID
     */
    private Long saveKanjiTask(String fileName, String s3Key) {
        Kanji kanji = Kanji.builder().bookName(Optional.ofNullable(fileName).orElse("unnamed")).s3Key(s3Key).status(TaskStatus.PENDING).build();

        repository.save(kanji);

        log.info("작업 생성 완료 - id: {}, 상태: {}, 코드: {}, S3 키: {}", kanji.getId(), TaskStatus.PENDING.getStatus(), TaskStatus.PENDING.getCode(), s3Key);

        return kanji.getId();
    }

    @Transactional
    public void processKanjiData(String bookName, Object kanjiDetails) {
        log.info("Python에서 처리된 책 데이터 수신 - book_name: {}, 항목 수: {}", bookName, (kanjiDetails instanceof Iterable) ? ((Iterable<?>) kanjiDetails).spliterator().getExactSizeIfKnown() : -1);

        // 경로 정규화: 다양한 경로 형식을 정리하여 일관된 키 포맷으로 변환
        String normalizedKey = bookName;

        // "./s3PDF/" 형식 처리
        if (normalizedKey.startsWith("./s3PDF/")) {
            normalizedKey = normalizedKey.substring(2); // "./" 제거
        }

        // "s3PDF/" 형식 처리하여 파일명만 추출
        if (normalizedKey.contains("s3PDF/")) {
            normalizedKey = normalizedKey.substring(normalizedKey.indexOf("s3PDF/") + 6);
        }

        // 최종 S3 키 (파일명)
        String s3Key = normalizedKey;

        log.info("정규화된 S3 키: {}", s3Key);

        // 해당 S3 키로 Kanji 엔티티 찾기
        Optional<Kanji> kanjiOpt = repository.findByS3Key(s3Key);

        if (kanjiOpt.isPresent()) {
            Kanji kanji = kanjiOpt.get();
            Map<String, Object> bookData = new HashMap<>();
            bookData.put("details", kanjiDetails);

            kanji.completeProcessing(bookData);

            repository.save(kanji); // 명시적 저장 추가

            log.info("작업 완료 - id: {}, 상태: {}, 코드: {}", kanji.getId(), TaskStatus.DONE.getStatus(), TaskStatus.DONE.getCode());
        } else {
            // 다른 키 형식도 시도해 보기
            log.warn("정규화된 S3 키로 작업을 찾을 수 없음. 원본 키 시도: {}", bookName);
            kanjiOpt = repository.findByS3Key(bookName);

            if (kanjiOpt.isPresent()) {
                Kanji kanji = kanjiOpt.get();
                Map<String, Object> bookData = new HashMap<>();
                bookData.put("details", kanjiDetails);
                bookData.put("bookName", bookName);

                kanji.completeProcessing(bookData);
                repository.save(kanji); // 명시적 저장 추가

                log.info("작업 완료 (원본 키 사용) - id: {}, 상태: {}, 코드: {}", kanji.getId(), TaskStatus.DONE.getStatus(), TaskStatus.DONE.getCode());
            } else {
                log.error("어떤 키로도 작업을 찾을 수 없음. 정규화된 키: {}, 원본 키: {}", s3Key, bookName);
            }
        }
    }

    public Kanji get(Long id) {
        Kanji kanji = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

        TaskStatus status = kanji.getStatus();
        log.info("작업 조회 - id: {}, 상태: {}, 코드: {}", id, status.getStatus(), status.getCode());

        return kanji;
    }

    /* Test을 위한 메소드 추후 삭제 예정 */
    public Kanji getKanjis() {
        return repository.findById(22L).orElseThrow(() -> new IllegalArgumentException("Test entry not found"));
    }
}