package backend_lingua.linguas.domain.oauth.api;

import backend_lingua.linguas.domain.oauth.dto.LoginResponse;
import backend_lingua.linguas.domain.oauth.enumerated.ProviderType;
import backend_lingua.linguas.domain.oauth.service.AuthService;

import backend_lingua.linguas.infrastructure.security.token.dto.TokenInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "v1-auth", description = "회원 인증을 담당하는 API")
public class MobileAuthController {

    private final AuthService authService;

    @Operation(
            summary = "소셜 로그인",
            description = "제공처의 액세스 토큰으로 회원 정보 조회, 로그인 처리 후 액세스 토큰과 회원 정보를 응답으로 전송"
    )
    @PostMapping(value = "/login/{provider}")
    public ResponseEntity<LoginResponse> login(
            @Parameter(description = "소셜 로그인 제공자 (kakao, naver)", required = true)
            @PathVariable("provider") ProviderType provider,
            @RequestBody String providerAccessToken) {

        LoginResponse loginResponse = authService.socialLogin(provider, providerAccessToken);

        return ResponseEntity.ok(loginResponse);
    }

    @Operation(
            summary = "액세스 토큰 재발급",
            description = "리프레시 토큰이 유효하다면, 새로운 액세스 토큰 발급"
    )
    @PostMapping(value = "/reissue-token")
    public ResponseEntity<TokenInfo> reissueToken(@RequestBody String refreshToken) {

        TokenInfo tokenInfo = authService.reissueToken(refreshToken);

        return ResponseEntity.ok(tokenInfo);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token을 삭제하여 로그아웃 처리")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshTokenHeader) {

        authService.logout(refreshTokenHeader);

        return ResponseEntity.ok().build();
    }
}