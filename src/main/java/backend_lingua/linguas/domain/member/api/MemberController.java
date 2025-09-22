package backend_lingua.linguas.domain.member.api;

import backend_lingua.linguas.domain.member.dto.MemberInfo;
import backend_lingua.linguas.infrastructure.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

    @GetMapping("/user/profile")
    public ResponseEntity<MemberInfo> getUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Get User Profile = {}" ,userPrincipal.toString() );

        MemberInfo member = MemberInfo.from(userPrincipal.getMember());

        return ResponseEntity.ok(member);
    }


    @Operation(
            summary = "얕은 삭제된 단어장 파일 조회",
            description = "deleteAt(삭제일자) 기준으로 소프트 삭제 처리된 단어장 파일을 조회합니다.")
    @GetMapping("/{bookName}")
    public ResponseEntity<Void>  getDeleteVocabulary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String bookName) {


        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "업로드 파일 삭제",
            description = "업로드 한 파일 삭제"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void>  deleteVocabulary(@PathVariable Long id) {
        
        return ResponseEntity.noContent().build();
    }


}