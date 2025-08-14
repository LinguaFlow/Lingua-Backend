package backend_lingua.linguas.oauth.service;

import backend_lingua.linguas.security.dto.response.LoginResponse;
import backend_lingua.linguas.security.dto.response.TokenInfo;
import backend_lingua.linguas.security.principal.UserPrincipal;

public interface AuthService {
    // OAuth 로그인 처리
    LoginResponse createTokensForOAuth(UserPrincipal userPrincipal);

    // 카카오 로그인 처리 (Android용)
    LoginResponse processKakaoLogin(String kakaoToken, String email, String name, String profileImage);

    // 토큰 재발급
    TokenInfo reissueToken(String refreshToken);

    // 로그아웃
    void logout(String email);

    // 토큰 검증
    boolean validateToken(String token);

    // 토큰에서 이메일 추출
    String getEmailFromToken(String token);

    // 토큰에서 사용자 정보 추출
    UserPrincipal getUserFromToken(String token);
}

