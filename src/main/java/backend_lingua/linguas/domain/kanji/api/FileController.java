package backend_lingua.linguas.domain.kanji.api;

import backend_lingua.linguas.domain.kanji.dto.response.FileUploadResponse;
import backend_lingua.linguas.domain.kanji.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.domain.kanji.entity.File;
import backend_lingua.linguas.domain.kanji.service.FileServiceImpl;
import backend_lingua.linguas.infrastructure.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileServiceImpl fileServiceImpl;

    @Operation(
            summary = "PDF 파일 업로드",
            description = "학습중인 단어장 파일 업로드"
    )
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> upload(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam MultipartFile file
    ) {

        FileUploadResponse kanji = fileServiceImpl.uploadFile(userPrincipal.getMember(), file);

        return ResponseEntity.accepted().body(kanji);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<FileUploadResponse> status(
            @PathVariable Long id
    ) {

        FileUploadResponse uploadStatus = fileServiceImpl.uploadStatus(id);

        return ResponseEntity.ok(uploadStatus);
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<KanjiVocabularyListResponse> result(
            @PathVariable Long id
    ) {
        File file = fileServiceImpl.get(id);

        return file.getStatus().buildResultResponse(file);
    }
}