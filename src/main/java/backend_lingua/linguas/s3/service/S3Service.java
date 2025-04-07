package backend_lingua.linguas.s3.service;

import backend_lingua.linguas.s3.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Properties s3Properties;
    private final S3Client s3Client;

    public void update(MultipartFile file, String keyName) {
        String fileExtension = getFileExtension(keyName);
        String newPdfFileName = keyName + fileExtension;
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getS3().getBucket())
                    .key(newPdfFileName)
                    .contentLength(file.getSize())
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("파일 업로드 성공 - key: {}", newPdfFileName);
        } catch (IOException e) {
            log.error("파일 업로드 실패 - key: {}", newPdfFileName, e);
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
