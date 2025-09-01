package backend_lingua.linguas.domain.vocabulary.api;

import backend_lingua.linguas.domain.vocabulary.dto.response.UploadTaskStatusResponse;
import backend_lingua.linguas.domain.vocabulary.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.domain.vocabulary.service.VocabularyWordService;
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
public class VocabularyWordController {

    private final VocabularyWordService vocabularyWordService;

    @Operation(
            summary = "PDF 파일 업로드",
            description = "학습중인 단어장 파일 업로드"
    )
    @PostMapping("/upload")
    public ResponseEntity<UploadTaskStatusResponse> upload(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam MultipartFile file
    ) {

        UploadTaskStatusResponse kanji = vocabularyWordService.uploadFile(userPrincipal.getMember(), file);

        return ResponseEntity.accepted().body(kanji);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<UploadTaskStatusResponse> status(
            @PathVariable Long id
    ) {

        UploadTaskStatusResponse uploadStatus = vocabularyWordService.uploadTaskStatus(id);

        return ResponseEntity.ok(uploadStatus);
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<KanjiVocabularyListResponse> result(
            @PathVariable Long id
    ) {

        KanjiVocabularyListResponse completedTask = vocabularyWordService.getCompletedTask(id);
        return ResponseEntity.ok(completedTask);
    }
}