package backend_lingua.linguas.oauth.controller;

import backend_lingua.linguas.oauth.service.AuthService;
import backend_lingua.linguas.security.dto.response.ApiResponse;
import backend_lingua.linguas.security.dto.response.LoginResponse;
import backend_lingua.linguas.security.dto.response.TokenInfo;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@Slf4j
@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MobileAuthController {

    private final AuthService authService;

    /**
     * 카카오 로그인 (Android)
     */
    @PostMapping("/auth/kakao")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request) {

        log.info("Android 카카오 로그인 요청: {}", request.getEmail());

        try {
            LoginResponse response = authService.processKakaoLogin(
                    request.getAccessToken(),
                    request.getEmail(),
                    request.getName(),
                    request.getProfileImage()
            );

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("카카오 로그인 실패", e);
            ApiResponse<LoginResponse> errorResponse = ApiResponse.error(
                    e.getMessage(),
                    HttpStatus.UNAUTHORIZED
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * 토큰 재발급 (Android)
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<TokenInfo>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        try {
            TokenInfo tokenInfo = authService.reissueToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success(tokenInfo));
        } catch (Exception e) {
            log.error("토큰 재발급 실패", e);
            ApiResponse<TokenInfo> errorResponse = ApiResponse.error(
                    e.getMessage(),
                    HttpStatus.UNAUTHORIZED
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * 토큰 검증
     */
    @GetMapping("/auth/validate")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                TokenValidationResponse response = new TokenValidationResponse(false, null);
                return ResponseEntity.ok(ApiResponse.success(response));
            }

            String token = authHeader.substring(7);
            boolean isValid = authService.validateToken(token);
            String email = isValid ? authService.getEmailFromToken(token) : null;

            TokenValidationResponse response = new TokenValidationResponse(isValid, email);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            TokenValidationResponse response = new TokenValidationResponse(false, null);
            return ResponseEntity.ok(ApiResponse.success(response));
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                authService.logout(email);
                return ResponseEntity.ok(ApiResponse.success(null, "로그아웃 성공"));
            }

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "Invalid token",
                    HttpStatus.BAD_REQUEST
            );
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("로그아웃 실패", e);
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Request DTOs
    @Data
    public static class KakaoLoginRequest {
//        @NotBlank(message = "Access token is required")
        private String accessToken;

//        @NotBlank(message = "Email is required")
//        @Email(message = "유효한 이메일 형식이어야 합니다")
        private String email;

        private String name;
        private String profileImage;
        private String clientType = "android";
    }

    @Data
    public static class RefreshTokenRequest {
//        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    @AllArgsConstructor
    public static class TokenValidationResponse {
        private boolean valid;
        private String email;
    }
}
