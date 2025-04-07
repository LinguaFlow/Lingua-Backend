package backend_lingua.linguas.s3.api;

import backend_lingua.linguas.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam(value = "file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();

        // 로그 추가
        log.info("파일 업로드 요청 - 파일명: {}, 크기: {}", originalFilename, fileSize);

        try {
            s3Service.update(file, originalFilename);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "파일 업로드 성공! 파일명: " + originalFilename + ", 크기: " + fileSize);
            response.put("filename", originalFilename);
            response.put("size", fileSize);

            log.info("파일 업로드 성공 - 파일명: {}, 크기: {}", originalFilename, fileSize);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("파일 업로드 실패 - 파일명: {}, 오류: {}", originalFilename, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("파일 업로드 실패: " + e.getMessage());
        }
    }
}