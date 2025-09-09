package backend_lingua.linguas.domain.vocabulary.api;

import backend_lingua.linguas.domain.vocabulary.dto.response.UploadTaskStatusResponse;
import backend_lingua.linguas.domain.vocabulary.dto.response.KanjiVocabularyListResponse;
import backend_lingua.linguas.domain.vocabulary.service.VocabularyWordService;
import backend_lingua.linguas.infrastructure.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/files")
@Tag(name = "Vocabulary Upload", description = "학습용 단어장(PDF) 업로드 및 처리 상태/결과 조회 API")
@RequiredArgsConstructor
public class VocabularyWordController {

    private final VocabularyWordService vocabularyWordService;

    @Operation(
            summary = "PDF 업로드 요청 (비동기 처리)",
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

    @Operation(
            summary = "업로드/처리 상태 조회",
            description = "업로드된 작업의 현재 상태를 조회합니다"
    )
    @GetMapping("/{id}/status")
    public ResponseEntity<UploadTaskStatusResponse> status(
            @PathVariable Long id
    ) {

        UploadTaskStatusResponse uploadStatus = vocabularyWordService.uploadTaskStatus(id);

        return ResponseEntity.ok(uploadStatus);
    }

    @Operation(
            summary = "PDF 파일 업로드",
            description = "작업이 `DONE` 상태일 때, 추출/정규화된 한자/어휘 리스트 결과를 반환합니다"
    )
    @GetMapping("/{id}/result")
    public ResponseEntity<KanjiVocabularyListResponse> result(
            @PathVariable Long id
    ) {
        KanjiVocabularyListResponse completedTask = vocabularyWordService.getCompletedTask(id);
        return ResponseEntity.ok(completedTask);
    }

    @Operation(
            summary = "업로드 작업 취소",
            description = "진행 중인 작업을 취소하고 관련 리소스를 정리합니다"
    )
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelUpload(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        vocabularyWordService.cancelUpload(userPrincipal.getMember(), id);
        return ResponseEntity.noContent().build();
    }
}