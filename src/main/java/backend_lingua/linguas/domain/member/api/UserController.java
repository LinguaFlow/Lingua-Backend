package backend_lingua.linguas.domain.member.api;

import backend_lingua.linguas.domain.member.dto.MemberInfo;
import backend_lingua.linguas.infrastructure.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/user/profile")
    public ResponseEntity<MemberInfo> getUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Get User Profile = {}" ,userPrincipal.toString() );

        MemberInfo member = MemberInfo.from(userPrincipal.getMember());

        return ResponseEntity.ok(member);
    }

}